package it.alessiogori.battledebrief.unit.dto;

import java.math.BigDecimal;
import java.util.List;

public record UnitResponse(
        Long id,
        String externalUnitId,
        String name,
        String faction,
        String category,
        Integer baseCost,
        String description,
        Integer hitPoints,
        BigDecimal speed,
        String armor,
        String mainWeapon,
        String imageUrl,
        String datasetVersion,
        List<SpecializationResponse> specializations
) {

    public UnitResponse {
        specializations = List.copyOf(specializations);
    }
}
