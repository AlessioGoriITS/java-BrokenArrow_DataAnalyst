package it.alessiogori.battledebrief.match.mapper;

import it.alessiogori.battledebrief.match.dto.MatchDetailResponse;
import it.alessiogori.battledebrief.match.dto.MatchPerformanceResponse;
import it.alessiogori.battledebrief.match.dto.PlayerMatchSummaryResponse;
import it.alessiogori.battledebrief.match.dto.UnitMatchPerformanceResponse;
import it.alessiogori.battledebrief.match.entity.GameMatch;
import it.alessiogori.battledebrief.match.entity.MatchPerformance;
import it.alessiogori.battledebrief.match.entity.UnitMatchPerformance;
import org.springframework.stereotype.Component;

@Component
public class MatchMapper {

    public PlayerMatchSummaryResponse toSummary(
            GameMatch match,
            Long playerProfileId
    ) {
        MatchPerformance performance = match.getPerformances().stream()
                .filter(item -> item.getPlayerProfile().getId()
                        .equals(playerProfileId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Player performance is missing from filtered match"
                ));
        return new PlayerMatchSummaryResponse(
                match.getId(),
                match.getExternalMatchId(),
                match.getMapName(),
                match.getGameMode(),
                match.getStartedAt(),
                match.getDurationSeconds(),
                match.getWinnerTeam(),
                match.getSource(),
                performance.getTeam(),
                performance.isWon(),
                performance.getOldRating(),
                performance.getNewRating(),
                performance.getDestructionScore(),
                performance.getLossesScore()
        );
    }

    public MatchDetailResponse toDetail(GameMatch match) {
        return new MatchDetailResponse(
                match.getId(),
                match.getExternalMatchId(),
                match.getMapName(),
                match.getGameMode(),
                match.getStartedAt(),
                match.getDurationSeconds(),
                match.getWinnerTeam(),
                match.getSource(),
                match.getImportedAt(),
                match.getPerformances().stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    private MatchPerformanceResponse toResponse(
            MatchPerformance performance
    ) {
        return new MatchPerformanceResponse(
                performance.getPlayerProfile().getId(),
                performance.getPlayerProfile().getDisplayName(),
                performance.getTeam(),
                performance.isWon(),
                performance.getOldRating(),
                performance.getNewRating(),
                performance.getDestructionScore(),
                performance.getLossesScore(),
                performance.getDamageDealt(),
                performance.getDamageReceived(),
                performance.getObjectivesCaptured(),
                performance.getSpawnedUnitScore(),
                performance.getRefundedUnitScore(),
                performance.getSupplyConsumed(),
                performance.getUnitPerformances().stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    private UnitMatchPerformanceResponse toResponse(
            UnitMatchPerformance performance
    ) {
        return new UnitMatchPerformanceResponse(
                performance.getUnit().getId(),
                performance.getUnit().getExternalUnitId(),
                performance.getUnit().getName(),
                performance.getUnitCost(),
                performance.getSpawnedCount(),
                performance.getLostCount(),
                performance.getKillsCount(),
                performance.getDestroyedValue(),
                performance.getDamageDealt(),
                performance.getDamageReceived(),
                performance.getSupplyConsumed()
        );
    }
}
