package it.alessiogori.battledebrief.analytics.dto;

import java.time.Instant;

public record PlayerTrendPointResponse(
        Long matchId,
        String externalMatchId,
        String mapName,
        Instant startedAt,
        boolean won,
        Integer oldRating,
        Integer newRating,
        Integer eloChange,
        long destroyedValue,
        long lostValue,
        long deploymentValue,
        RatioMetric economicKd,
        RatioMetric deploymentEfficiency,
        RatioMetric damageRatio
) {
}
