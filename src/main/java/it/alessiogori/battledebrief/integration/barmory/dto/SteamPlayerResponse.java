package it.alessiogori.battledebrief.integration.barmory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SteamPlayerResponse(
        String steamId,
        long commanderId,
        String displayName,
        int level,
        BigDecimal currentRating,
        int leaderboardRank,
        SteamCareerResponse career,
        List<SteamMatchResponse> recentMatches,
        List<SteamUnitPerformanceResponse> mostUsedUnits,
        Instant sourceUpdatedAt
) {
}
