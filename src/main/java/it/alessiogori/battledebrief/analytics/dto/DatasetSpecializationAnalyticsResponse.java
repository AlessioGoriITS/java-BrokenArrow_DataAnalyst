package it.alessiogori.battledebrief.analytics.dto;

public record DatasetSpecializationAnalyticsResponse(
        Long specializationId,
        String specializationName,
        String faction,
        long sampleMatches,
        long samplePlayers,
        long sampleUnits,
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
