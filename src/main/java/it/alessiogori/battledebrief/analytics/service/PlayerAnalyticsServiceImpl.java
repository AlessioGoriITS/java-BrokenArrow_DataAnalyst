package it.alessiogori.battledebrief.analytics.service;

import it.alessiogori.battledebrief.analytics.dto.AnalyticsStatus;
import it.alessiogori.battledebrief.analytics.dto.PlayerAnalysisResponse;
import it.alessiogori.battledebrief.analytics.dto.PlayerTrendPointResponse;
import it.alessiogori.battledebrief.analytics.repository.PlayerCareerAggregate;
import it.alessiogori.battledebrief.common.exception.ResourceNotFoundException;
import it.alessiogori.battledebrief.match.entity.MatchPerformance;
import it.alessiogori.battledebrief.match.repository.MatchPerformanceRepository;
import it.alessiogori.battledebrief.player.entity.PlayerProfile;
import it.alessiogori.battledebrief.player.repository.PlayerProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Service
public class PlayerAnalyticsServiceImpl implements PlayerAnalyticsService {

    private final PlayerProfileRepository playerProfileRepository;
    private final MatchPerformanceRepository performanceRepository;
    private final AnalyticsCalculator calculator;

    public PlayerAnalyticsServiceImpl(
            PlayerProfileRepository playerProfileRepository,
            MatchPerformanceRepository performanceRepository,
            AnalyticsCalculator calculator
    ) {
        this.playerProfileRepository = playerProfileRepository;
        this.performanceRepository = performanceRepository;
        this.calculator = calculator;
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerAnalysisResponse analyze(Long playerProfileId) {
        PlayerProfile player = playerProfileRepository
                .findById(playerProfileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Player profile not found"
                ));
        PlayerCareerAggregate aggregate = performanceRepository
                .aggregateCareer(playerProfileId);

        long matches = value(aggregate.getMatchCount());
        long wins = value(aggregate.getWinCount());
        long destroyedValue = value(aggregate.getDestroyedValue());
        long lostValue = value(aggregate.getLostValue());
        long damageDealt = value(aggregate.getDamageDealt());
        long damageReceived = value(aggregate.getDamageReceived());
        long deploymentValue = value(aggregate.getDeploymentValue());

        Integer initialElo = performanceRepository
                .findFirstByPlayerProfileIdAndOldRatingIsNotNullOrderByGameMatchStartedAtAsc(
                        playerProfileId
                )
                .map(MatchPerformance::getOldRating)
                .orElse(null);
        Integer currentElo = performanceRepository
                .findFirstByPlayerProfileIdAndNewRatingIsNotNullOrderByGameMatchStartedAtDesc(
                        playerProfileId
                )
                .map(MatchPerformance::getNewRating)
                .orElse(player.getCurrentElo());
        Integer peakElo = maxNullable(
                player.getPeakElo(),
                aggregate.getPeakElo(),
                currentElo
        );
        Integer eloChange = initialElo == null || currentElo == null
                ? null
                : currentElo - initialElo;

        return new PlayerAnalysisResponse(
                player.getId(),
                player.getDisplayName(),
                matches,
                wins,
                matches - wins,
                calculator.percentage(
                        wins,
                        matches,
                        AnalyticsStatus.NO_MATCHES
                ),
                initialElo,
                currentElo,
                peakElo,
                eloChange,
                destroyedValue,
                lostValue,
                calculator.average(destroyedValue, matches),
                calculator.average(lostValue, matches),
                calculator.ratio(
                        destroyedValue,
                        lostValue,
                        unavailableStatus(matches, AnalyticsStatus.NO_LOSSES)
                ),
                calculator.ratio(
                        destroyedValue,
                        deploymentValue,
                        unavailableStatus(
                                matches,
                                AnalyticsStatus.NO_DEPLOYMENTS
                        )
                ),
                calculator.ratio(
                        damageDealt,
                        damageReceived,
                        unavailableStatus(
                                matches,
                                AnalyticsStatus.NO_DAMAGE_RECEIVED
                        )
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerTrendPointResponse> trend(
            Long playerProfileId,
            int limit
    ) {
        requirePlayer(playerProfileId);
        List<MatchPerformance> recent = new ArrayList<>(
                performanceRepository.findAllByPlayerProfileId(
                        playerProfileId,
                        PageRequest.of(
                                0,
                                limit,
                                Sort.by(Sort.Direction.DESC,
                                        "gameMatch.startedAt")
                        )
                )
        );
        Collections.reverse(recent);
        return recent.stream().map(this::toTrendPoint).toList();
    }

    private PlayerTrendPointResponse toTrendPoint(
            MatchPerformance performance
    ) {
        long destroyed = value(performance.getDestructionScore());
        long lost = value(performance.getLossesScore());
        long deployed = value(performance.getSpawnedUnitScore());
        long damageDealt = value(performance.getDamageDealt());
        long damageReceived = value(performance.getDamageReceived());
        Integer eloChange = performance.getOldRating() == null
                || performance.getNewRating() == null
                ? null
                : performance.getNewRating() - performance.getOldRating();

        return new PlayerTrendPointResponse(
                performance.getGameMatch().getId(),
                performance.getGameMatch().getExternalMatchId(),
                performance.getGameMatch().getMapName(),
                performance.getGameMatch().getStartedAt(),
                performance.isWon(),
                performance.getOldRating(),
                performance.getNewRating(),
                eloChange,
                destroyed,
                lost,
                deployed,
                calculator.ratio(
                        destroyed,
                        lost,
                        AnalyticsStatus.NO_LOSSES
                ),
                calculator.ratio(
                        destroyed,
                        deployed,
                        AnalyticsStatus.NO_DEPLOYMENTS
                ),
                calculator.ratio(
                        damageDealt,
                        damageReceived,
                        AnalyticsStatus.NO_DAMAGE_RECEIVED
                )
        );
    }

    private void requirePlayer(Long playerProfileId) {
        if (!playerProfileRepository.existsById(playerProfileId)) {
            throw new ResourceNotFoundException("Player profile not found");
        }
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private AnalyticsStatus unavailableStatus(
            long matches,
            AnalyticsStatus denominatorStatus
    ) {
        return matches == 0
                ? AnalyticsStatus.NO_MATCHES
                : denominatorStatus;
    }

    private Integer maxNullable(Integer... values) {
        return Stream.of(values)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .orElse(null);
    }
}
