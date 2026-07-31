package it.alessiogori.battledebrief.analytics.dto;

public record PlayerUnitAnalysisResponse(
        Long unitId,
        String externalUnitId,
        String unitName,
        String faction,
        String category,
        long sampleMatches,
        long spawnedCount,
        long lostCount,
        long killsCount,
        long destroyedValue,
        long deploymentCost,
        long lostValue,
        long damageDealt,
        long damageReceived,
        long supplyConsumed,
        RatioMetric economicKd,
        RatioMetric deploymentEfficiency,
        RatioMetric survivalRate,
        RatioMetric damageRatio
) {
}
