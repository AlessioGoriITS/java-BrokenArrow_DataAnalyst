package it.alessiogori.battledebrief.match.service;

import it.alessiogori.battledebrief.common.exception.ImportValidationException;
import it.alessiogori.battledebrief.common.exception.ResourceNotFoundException;
import it.alessiogori.battledebrief.match.dto.ImportedMatchRequest;
import it.alessiogori.battledebrief.match.dto.MatchImportRequest;
import it.alessiogori.battledebrief.match.dto.MatchImportResponse;
import it.alessiogori.battledebrief.match.dto.MatchPerformanceImportRequest;
import it.alessiogori.battledebrief.match.dto.UnitPerformanceImportRequest;
import it.alessiogori.battledebrief.match.entity.GameMatch;
import it.alessiogori.battledebrief.match.entity.MatchPerformance;
import it.alessiogori.battledebrief.match.entity.MatchSource;
import it.alessiogori.battledebrief.match.entity.UnitMatchPerformance;
import it.alessiogori.battledebrief.match.repository.GameMatchRepository;
import it.alessiogori.battledebrief.player.entity.PlayerProfile;
import it.alessiogori.battledebrief.player.repository.PlayerProfileRepository;
import it.alessiogori.battledebrief.unit.entity.Unit;
import it.alessiogori.battledebrief.unit.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Validated
public class MatchImportServiceImpl implements MatchImportService {

    private final GameMatchRepository gameMatchRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final UnitRepository unitRepository;

    public MatchImportServiceImpl(
            GameMatchRepository gameMatchRepository,
            PlayerProfileRepository playerProfileRepository,
            UnitRepository unitRepository
    ) {
        this.gameMatchRepository = gameMatchRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.unitRepository = unitRepository;
    }

    @Override
    @Transactional
    public MatchImportResponse importMatches(MatchImportRequest request) {
        validateUniqueMatchIds(request.matches());
        PlayerProfile player = playerProfileRepository
                .findById(request.playerProfileId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Player profile not found"
                ));

        List<Long> importedIds = new ArrayList<>();
        List<String> skippedIds = new ArrayList<>();
        for (ImportedMatchRequest importedMatch : request.matches()) {
            String externalMatchId = importedMatch.externalMatchId().trim();
            if (gameMatchRepository.existsByExternalMatchId(externalMatchId)) {
                skippedIds.add(externalMatchId);
                continue;
            }

            validatePerformance(importedMatch);
            GameMatch match = toEntity(importedMatch, player);
            importedIds.add(gameMatchRepository.save(match).getId());
        }
        gameMatchRepository.flush();

        return new MatchImportResponse(
                importedIds.size(),
                skippedIds.size(),
                importedIds,
                skippedIds
        );
    }

    private GameMatch toEntity(
            ImportedMatchRequest request,
            PlayerProfile player
    ) {
        GameMatch match = new GameMatch(
                request.externalMatchId().trim(),
                request.mapName().trim(),
                request.gameMode().trim(),
                request.startedAt(),
                request.durationSeconds(),
                request.winnerTeam(),
                MatchSource.JSON_IMPORT
        );
        MatchPerformanceImportRequest importedPerformance =
                request.performance();
        MatchPerformance performance = new MatchPerformance(
                player,
                importedPerformance.team(),
                importedPerformance.won()
        );
        performance.updateMetrics(
                importedPerformance.oldRating(),
                importedPerformance.newRating(),
                importedPerformance.destructionScore(),
                importedPerformance.lossesScore(),
                importedPerformance.damageDealt(),
                importedPerformance.damageReceived(),
                importedPerformance.objectivesCaptured(),
                importedPerformance.spawnedUnitScore(),
                importedPerformance.refundedUnitScore(),
                importedPerformance.supplyConsumed()
        );
        importedPerformance.units().forEach(unit ->
                performance.addUnitPerformance(toEntity(unit))
        );
        match.addPerformance(performance);
        return match;
    }

    private UnitMatchPerformance toEntity(
            UnitPerformanceImportRequest request
    ) {
        String externalUnitId = request.externalUnitId().trim();
        Unit unit = unitRepository.findByExternalUnitId(externalUnitId)
                .orElseThrow(() -> new ImportValidationException(
                        "Unknown unit externalUnitId: " + externalUnitId
                ));
        UnitMatchPerformance performance = new UnitMatchPerformance(
                unit,
                request.unitCost(),
                request.spawnedCount(),
                request.lostCount()
        );
        performance.updateMetrics(
                request.killsCount(),
                request.destroyedValue(),
                request.damageDealt(),
                request.damageReceived(),
                request.supplyConsumed()
        );
        return performance;
    }

    private void validateUniqueMatchIds(List<ImportedMatchRequest> matches) {
        Set<String> externalIds = new HashSet<>();
        for (ImportedMatchRequest match : matches) {
            String externalId = match.externalMatchId().trim();
            if (!externalIds.add(externalId)) {
                throw new ImportValidationException(
                        "Duplicate externalMatchId in import: " + externalId
                );
            }
        }
    }

    private void validatePerformance(ImportedMatchRequest match) {
        MatchPerformanceImportRequest performance = match.performance();
        if (match.winnerTeam() != null
                && performance.won()
                != (match.winnerTeam() == performance.team())) {
            throw new ImportValidationException(
                    "Performance result is inconsistent for match "
                            + match.externalMatchId()
            );
        }

        Set<String> unitIds = new HashSet<>();
        for (UnitPerformanceImportRequest unit : performance.units()) {
            String externalUnitId = unit.externalUnitId().trim();
            if (!unitIds.add(externalUnitId)) {
                throw new ImportValidationException(
                        "Duplicate unit " + externalUnitId + " in match "
                                + match.externalMatchId()
                );
            }
            if (unit.lostCount() > unit.spawnedCount()) {
                throw new ImportValidationException(
                        "lostCount cannot exceed spawnedCount for unit "
                                + externalUnitId
                );
            }
        }
    }
}
