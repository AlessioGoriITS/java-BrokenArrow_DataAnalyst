package it.alessiogori.battledebrief.integration.barmory.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SteamMatchResponse(
        long matchId,
        int mapId,
        String mapName,
        boolean won,
        int teamId,
        int durationSeconds,
        Instant endedAt,
        BigDecimal oldRating,
        BigDecimal newRating,
        int destructionScore,
        int lossesScore,
        int damageDealt,
        int damageReceived,
        int objectivesCaptured,
        int experience
) {
}
