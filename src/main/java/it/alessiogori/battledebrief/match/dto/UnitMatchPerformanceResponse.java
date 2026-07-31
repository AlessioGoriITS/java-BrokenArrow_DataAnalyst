package it.alessiogori.battledebrief.match.dto;

public record UnitMatchPerformanceResponse(
        Long unitId,
        String externalUnitId,
        String unitName,
        Integer unitCost,
        Integer spawnedCount,
        Integer lostCount,
        Integer killsCount,
        Long destroyedValue,
        Long damageDealt,
        Long damageReceived,
        Long supplyConsumed
) {
}
