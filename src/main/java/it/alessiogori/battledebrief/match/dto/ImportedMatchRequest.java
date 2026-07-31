package it.alessiogori.battledebrief.match.dto;

import it.alessiogori.battledebrief.match.entity.TeamSide;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ImportedMatchRequest(
        @NotBlank @Size(max = 100) String externalMatchId,
        @NotBlank @Size(max = 150) String mapName,
        @NotBlank @Size(max = 100) String gameMode,
        @NotNull Instant startedAt,
        @NotNull @Positive Integer durationSeconds,
        TeamSide winnerTeam,
        @NotNull @Valid MatchPerformanceImportRequest performance
) {
}
