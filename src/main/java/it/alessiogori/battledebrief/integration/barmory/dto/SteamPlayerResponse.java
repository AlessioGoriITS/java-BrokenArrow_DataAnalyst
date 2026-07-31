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
        String source,
        Instant sourceUpdatedAt,
        SteamLookupDiagnosticsResponse diagnostics
) {

    public SteamPlayerResponse(
            String steamId,
            long commanderId,
            String displayName,
            int level,
            BigDecimal currentRating,
            int leaderboardRank,
            SteamCareerResponse career,
            List<SteamMatchResponse> recentMatches,
            List<SteamUnitPerformanceResponse> mostUsedUnits,
            String source,
            Instant sourceUpdatedAt
    ) {
        this(
                steamId,
                commanderId,
                displayName,
                level,
                currentRating,
                leaderboardRank,
                career,
                recentMatches,
                mostUsedUnits,
                source,
                sourceUpdatedAt,
                SteamLookupDiagnosticsResponse.empty()
        );
    }

    public SteamPlayerResponse withDiagnostics(
            SteamLookupDiagnosticsResponse value
    ) {
        return new SteamPlayerResponse(
                steamId,
                commanderId,
                displayName,
                level,
                currentRating,
                leaderboardRank,
                career,
                recentMatches,
                mostUsedUnits,
                source,
                sourceUpdatedAt,
                value
        );
    }
}
