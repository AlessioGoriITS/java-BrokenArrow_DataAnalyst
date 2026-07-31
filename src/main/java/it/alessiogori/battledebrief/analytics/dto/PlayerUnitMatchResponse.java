package it.alessiogori.battledebrief.analytics.dto;

import java.time.Instant;

public record PlayerUnitMatchResponse(
        Long matchId,
        String externalMatchId,
        String mapName,
        Instant startedAt,
        boolean won,
        Integer newRating,
        Long unitId,
        String externalUnitId,
        String unitName,
        int unitCost,
        int spawnedCount,
        int lostCount,
        int killsCount,
        long destroyedValue,
        long deploymentCost,
        long lostValue,
        Long damageDealt,
        Long damageReceived,
        Long supplyConsumed,
        RatioMetric economicKd,
        RatioMetric deploymentEfficiency,
        RatioMetric survivalRate,
        RatioMetric damageRatio
) {
}
