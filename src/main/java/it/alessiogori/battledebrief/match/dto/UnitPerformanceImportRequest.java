package it.alessiogori.battledebrief.match.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UnitPerformanceImportRequest(
        @NotBlank @Size(max = 100) String externalUnitId,
        @NotNull @PositiveOrZero Integer unitCost,
        @NotNull @PositiveOrZero Integer spawnedCount,
        @NotNull @PositiveOrZero Integer lostCount,
        @NotNull @PositiveOrZero Integer killsCount,
        @NotNull @PositiveOrZero Long destroyedValue,
        @PositiveOrZero Long damageDealt,
        @PositiveOrZero Long damageReceived,
        @PositiveOrZero Long supplyConsumed
) {
}
