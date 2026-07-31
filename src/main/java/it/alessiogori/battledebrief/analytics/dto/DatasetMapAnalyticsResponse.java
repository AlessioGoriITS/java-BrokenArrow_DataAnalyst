package it.alessiogori.battledebrief.analytics.dto;

public record DatasetMapAnalyticsResponse(
        String mapName,
        long sampleMatches,
        long samplePlayers,
        long datasetMatches,
        long destroyedValue,
        long lostValue,
        long damageDealt,
        long damageReceived,
        long deploymentValue,
        RatioMetric playRate,
        RatioMetric winRate,
        RatioMetric economicKd,
        RatioMetric deploymentEfficiency,
        RatioMetric damageRatio
) {
}
