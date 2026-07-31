package it.alessiogori.battledebrief.analytics.dto;

public record DatasetUnitAnalyticsResponse(
        Long unitId,
        String externalUnitId,
        String unitName,
        String faction,
        String category,
        long sampleMatches,
        long samplePlayers,
        long datasetMatches,
        long spawnedCount,
        long lostCount,
        long destroyedValue,
        long deploymentCost,
        long lostValue,
        RatioMetric playRate,
        RatioMetric winRate,
        RatioMetric economicKd,
        RatioMetric deploymentEfficiency,
        RatioMetric survivalRate
) {
}
