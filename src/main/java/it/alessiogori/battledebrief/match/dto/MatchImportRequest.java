package it.alessiogori.battledebrief.match.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MatchImportRequest(
        @NotNull @Positive Long playerProfileId,
        @NotEmpty @Size(max = 100) List<@Valid ImportedMatchRequest> matches
) {

    public MatchImportRequest {
        matches = matches == null ? null : List.copyOf(matches);
    }
}
