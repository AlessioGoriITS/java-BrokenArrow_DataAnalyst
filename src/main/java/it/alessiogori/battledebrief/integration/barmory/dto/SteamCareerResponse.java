package it.alessiogori.battledebrief.integration.barmory.dto;

import java.math.BigDecimal;

public record SteamCareerResponse(
        int matches,
        int wins,
        int losses,
        int leaves,
        int kills,
        int deaths,
        BigDecimal kdRatio,
        BigDecimal winRate,
        long playTimeSeconds,
        int capturedZones,
        int friendlyFireKills,
        long suppliedPoints
) {
}
