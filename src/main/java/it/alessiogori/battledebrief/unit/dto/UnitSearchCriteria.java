package it.alessiogori.battledebrief.unit.dto;

public record UnitSearchCriteria(
        String name,
        String faction,
        String category,
        Long specializationId,
        Integer minCost,
        Integer maxCost
) {
}
