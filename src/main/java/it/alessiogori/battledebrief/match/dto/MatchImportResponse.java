package it.alessiogori.battledebrief.match.dto;

import java.util.List;

public record MatchImportResponse(
        int importedCount,
        int skippedCount,
        List<Long> importedMatchIds,
        List<String> skippedExternalMatchIds
) {

    public MatchImportResponse {
        importedMatchIds = List.copyOf(importedMatchIds);
        skippedExternalMatchIds = List.copyOf(skippedExternalMatchIds);
    }
}
