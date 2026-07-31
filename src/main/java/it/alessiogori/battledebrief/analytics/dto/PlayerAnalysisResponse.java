package it.alessiogori.battledebrief.analytics.dto;

import java.math.BigDecimal;

public record PlayerAnalysisResponse(
        Long playerProfileId,
        String displayName,
        long matchCount,
        long wins,
        long losses,
        RatioMetric winRate,
        Integer initialElo,
        Integer currentElo,
        Integer peakElo,
        Integer eloChange,
        long totalDestroyedValue,
        long totalLostValue,
        BigDecimal averageDestroyedValue,
        BigDecimal averageLostValue,
        RatioMetric economicKd,
        RatioMetric deploymentEfficiency,
        RatioMetric damageRatio
) {
}
