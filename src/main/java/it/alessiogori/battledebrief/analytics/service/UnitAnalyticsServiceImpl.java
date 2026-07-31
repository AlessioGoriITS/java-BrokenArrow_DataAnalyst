package it.alessiogori.battledebrief.analytics.service;

import it.alessiogori.battledebrief.analytics.dto.AnalyticsStatus;
import it.alessiogori.battledebrief.analytics.dto.PlayerUnitAnalysisResponse;
import it.alessiogori.battledebrief.analytics.dto.PlayerUnitMatchResponse;
import it.alessiogori.battledebrief.analytics.repository.PlayerUnitAggregate;
import it.alessiogori.battledebrief.common.dto.PageResponse;
import it.alessiogori.battledebrief.common.exception.ResourceNotFoundException;
import it.alessiogori.battledebrief.match.entity.GameMatch;
import it.alessiogori.battledebrief.match.entity.MatchPerformance;
import it.alessiogori.battledebrief.match.entity.UnitMatchPerformance;
import it.alessiogori.battledebrief.match.repository.UnitMatchPerformanceRepository;
import it.alessiogori.battledebrief.player.repository.PlayerProfileRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UnitAnalyticsServiceImpl implements UnitAnalyticsService {

    private final PlayerProfileRepository playerProfileRepository;
    private final UnitMatchPerformanceRepository performanceRepository;
    private final AnalyticsCalculator calculator;

    public UnitAnalyticsServiceImpl(
            PlayerProfileRepository playerProfileRepository,
            UnitMatchPerformanceRepository performanceRepository,
            AnalyticsCalculator calculator
    ) {
        this.playerProfileRepository = playerProfileRepository;
        this.performanceRepository = performanceRepository;
        this.calculator = calculator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerUnitAnalysisResponse> analyzeAll(Long playerProfileId) {
        requirePlayer(playerProfileId);
        return performanceRepository.aggregateByPlayer(playerProfileId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerUnitAnalysisResponse analyzeUnit(
            Long playerProfileId,
            Long unitId
    ) {
        requirePlayer(playerProfileId);
        return performanceRepository
                .aggregateByPlayerAndUnit(playerProfileId, unitId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unit performance not found for player"
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PlayerUnitMatchResponse> findUnitMatches(
            Long playerProfileId,
            Long unitId,
            Pageable pageable
    ) {
        requirePlayer(playerProfileId);
        if (!performanceRepository
                .existsByMatchPerformancePlayerProfileIdAndUnitId(
                        playerProfileId,
                        unitId
                )) {
            throw new ResourceNotFoundException(
                    "Unit performance not found for player"
            );
        }
        return PageResponse.from(performanceRepository
                .findAllByMatchPerformancePlayerProfileIdAndUnitId(
                        playerProfileId,
                        unitId,
                        pageable
                )
                .map(this::toMatchResponse));
    }

    private PlayerUnitAnalysisResponse toResponse(
            PlayerUnitAggregate aggregate
    ) {
        long spawned = value(aggregate.getSpawnedCount());
        long lost = value(aggregate.getLostCount());
        long destroyed = value(aggregate.getDestroyedValue());
        long deploymentCost = value(aggregate.getDeploymentCost());
        long lostValue = value(aggregate.getLostValue());
        long damageDealt = value(aggregate.getDamageDealt());
        long damageReceived = value(aggregate.getDamageReceived());

        return new PlayerUnitAnalysisResponse(
                aggregate.getUnitId(),
                aggregate.getExternalUnitId(),
                aggregate.getUnitName(),
                aggregate.getFaction(),
                aggregate.getCategory(),
                value(aggregate.getSampleMatches()),
                spawned,
                lost,
                value(aggregate.getKillsCount()),
                destroyed,
                deploymentCost,
                lostValue,
                damageDealt,
                damageReceived,
                value(aggregate.getSupplyConsumed()),
                calculator.ratio(
                        destroyed,
                        lostValue,
                        AnalyticsStatus.NO_LOSSES
                ),
                calculator.ratio(
                        destroyed,
                        deploymentCost,
                        AnalyticsStatus.NO_DEPLOYMENTS
                ),
                calculator.percentage(
                        spawned - lost,
                        spawned,
                        AnalyticsStatus.NO_DEPLOYMENTS
                ),
                calculator.ratio(
                        damageDealt,
                        damageReceived,
                        AnalyticsStatus.NO_DAMAGE_RECEIVED
                )
        );
    }

    private PlayerUnitMatchResponse toMatchResponse(
            UnitMatchPerformance performance
    ) {
        MatchPerformance playerPerformance =
                performance.getMatchPerformance();
        GameMatch match = playerPerformance.getGameMatch();
        long deploymentCost = (long) performance.getUnitCost()
                * performance.getSpawnedCount();
        long lostValue = (long) performance.getUnitCost()
                * performance.getLostCount();
        long damageDealt = value(performance.getDamageDealt());
        long damageReceived = value(performance.getDamageReceived());

        return new PlayerUnitMatchResponse(
                match.getId(),
                match.getExternalMatchId(),
                match.getMapName(),
                match.getStartedAt(),
                playerPerformance.isWon(),
                playerPerformance.getNewRating(),
                performance.getUnit().getId(),
                performance.getUnit().getExternalUnitId(),
                performance.getUnit().getName(),
                performance.getUnitCost(),
                performance.getSpawnedCount(),
                performance.getLostCount(),
                performance.getKillsCount(),
                performance.getDestroyedValue(),
                deploymentCost,
                lostValue,
                performance.getDamageDealt(),
                performance.getDamageReceived(),
                performance.getSupplyConsumed(),
                calculator.ratio(
                        performance.getDestroyedValue(),
                        lostValue,
                        AnalyticsStatus.NO_LOSSES
                ),
                calculator.ratio(
                        performance.getDestroyedValue(),
                        deploymentCost,
                        AnalyticsStatus.NO_DEPLOYMENTS
                ),
                calculator.percentage(
                        performance.getSpawnedCount()
                                - performance.getLostCount(),
                        performance.getSpawnedCount(),
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
}
