package it.alessiogori.battledebrief.match.dto;

import it.alessiogori.battledebrief.match.entity.TeamSide;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MatchPerformanceImportRequest(
        @NotNull TeamSide team,
        boolean won,
        @PositiveOrZero Integer oldRating,
        @PositiveOrZero Integer newRating,
        @PositiveOrZero Long destructionScore,
        @PositiveOrZero Long lossesScore,
        @PositiveOrZero Long damageDealt,
        @PositiveOrZero Long damageReceived,
        @PositiveOrZero Integer objectivesCaptured,
        @PositiveOrZero Long spawnedUnitScore,
        @PositiveOrZero Long refundedUnitScore,
        @PositiveOrZero Long supplyConsumed,
        @NotEmpty @Size(max = 200)
        List<@Valid UnitPerformanceImportRequest> units
) {

    public MatchPerformanceImportRequest {
        units = units == null ? null : List.copyOf(units);
    }
}
