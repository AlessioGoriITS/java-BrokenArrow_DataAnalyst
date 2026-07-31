package it.alessiogori.battledebrief.integration.barmory.dto;

import java.util.List;

public record SteamLookupDiagnosticsResponse(
        long durationMs,
        int requestedMatches,
        int loadedMatches,
        int discardedMatches,
        List<Long> discardedMatchIds,
        int invalidFields,
        List<String> warnings
) {

    public SteamLookupDiagnosticsResponse {
        discardedMatchIds = List.copyOf(discardedMatchIds);
        warnings = List.copyOf(warnings);
    }

    public static SteamLookupDiagnosticsResponse empty() {
        return new SteamLookupDiagnosticsResponse(
                0,
                0,
                0,
                0,
                List.of(),
                0,
                List.of()
        );
    }
}
