package it.alessiogori.battledebrief.analytics.service;

import it.alessiogori.battledebrief.analytics.dto.AnalyticsStatus;
import it.alessiogori.battledebrief.analytics.dto.DatasetMapAnalyticsResponse;
import it.alessiogori.battledebrief.analytics.dto.DatasetUnitAnalyticsResponse;
import it.alessiogori.battledebrief.analytics.repository.DatasetMapAggregate;
import it.alessiogori.battledebrief.analytics.repository.DatasetUnitAggregate;
import it.alessiogori.battledebrief.common.exception.ResourceNotFoundException;
import it.alessiogori.battledebrief.match.repository.GameMatchRepository;
import it.alessiogori.battledebrief.match.repository.MatchPerformanceRepository;
import it.alessiogori.battledebrief.match.repository.UnitMatchPerformanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DatasetAnalyticsServiceImpl implements DatasetAnalyticsService {

    private final UnitMatchPerformanceRepository performanceRepository;
    private final MatchPerformanceRepository matchPerformanceRepository;
    private final GameMatchRepository matchRepository;
    private final AnalyticsCalculator calculator;

    public DatasetAnalyticsServiceImpl(
            UnitMatchPerformanceRepository performanceRepository,
            MatchPerformanceRepository matchPerformanceRepository,
            GameMatchRepository matchRepository,
            AnalyticsCalculator calculator
    ) {
        this.performanceRepository = performanceRepository;
        this.matchPerformanceRepository = matchPerformanceRepository;
        this.matchRepository = matchRepository;
        this.calculator = calculator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatasetUnitAnalyticsResponse> analyzeUnits() {
        long datasetMatches = matchRepository.count();
        return performanceRepository.aggregateDatasetByUnit()
                .stream()
                .map(aggregate -> toResponse(aggregate, datasetMatches))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DatasetUnitAnalyticsResponse analyzeUnit(Long unitId) {
        long datasetMatches = matchRepository.count();
        return performanceRepository.aggregateDatasetByUnitId(unitId)
                .map(aggregate -> toResponse(aggregate, datasetMatches))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unit analytics not found in dataset"
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatasetMapAnalyticsResponse> analyzeMaps() {
        long datasetMatches = matchRepository.count();
        return matchPerformanceRepository.aggregateDatasetByMap()
                .stream()
                .map(aggregate -> toResponse(aggregate, datasetMatches))
                .toList();
    }

    private DatasetMapAnalyticsResponse toResponse(
            DatasetMapAggregate aggregate,
            long datasetMatches
    ) {
        long sampleMatches = value(aggregate.getSampleMatches());
        long destroyed = value(aggregate.getDestroyedValue());
        long lost = value(aggregate.getLostValue());
        long damageDealt = value(aggregate.getDamageDealt());
        long damageReceived = value(aggregate.getDamageReceived());
        long deployment = value(aggregate.getDeploymentValue());

        return new DatasetMapAnalyticsResponse(
                aggregate.getMapName(),
                sampleMatches,
                value(aggregate.getSamplePlayers()),
                datasetMatches,
                destroyed,
                lost,
                damageDealt,
                damageReceived,
                deployment,
                calculator.percentage(
                        sampleMatches,
                        datasetMatches,
                        AnalyticsStatus.NO_MATCHES
                ),
                calculator.percentage(
                        value(aggregate.getWonPerformances()),
                        value(aggregate.getSamplePerformances()),
                        AnalyticsStatus.NO_MATCHES
                ),
                calculator.ratio(
                        destroyed,
                        lost,
                        AnalyticsStatus.NO_LOSSES
                ),
                calculator.ratio(
                        destroyed,
                        deployment,
                        AnalyticsStatus.NO_DEPLOYMENTS
                ),
                calculator.ratio(
                        damageDealt,
                        damageReceived,
                        AnalyticsStatus.NO_DAMAGE_RECEIVED
                )
        );
    }

    private DatasetUnitAnalyticsResponse toResponse(
            DatasetUnitAggregate aggregate,
            long datasetMatches
    ) {
        long sampleMatches = value(aggregate.getSampleMatches());
        long spawned = value(aggregate.getSpawnedCount());
        long lost = value(aggregate.getLostCount());
        long destroyed = value(aggregate.getDestroyedValue());
        long deploymentCost = value(aggregate.getDeploymentCost());
        long lostValue = value(aggregate.getLostValue());

        return new DatasetUnitAnalyticsResponse(
                aggregate.getUnitId(),
                aggregate.getExternalUnitId(),
                aggregate.getUnitName(),
                aggregate.getFaction(),
                aggregate.getCategory(),
                sampleMatches,
                value(aggregate.getSamplePlayers()),
                datasetMatches,
                spawned,
                lost,
                destroyed,
                deploymentCost,
                lostValue,
                calculator.percentage(
                        sampleMatches,
                        datasetMatches,
                        AnalyticsStatus.NO_MATCHES
                ),
                calculator.percentage(
                        value(aggregate.getWonPerformances()),
                        value(aggregate.getSamplePerformances()),
                        AnalyticsStatus.NO_MATCHES
                ),
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
                )
        );
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
