package it.alessiogori.battledebrief.unit.dataset;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CatalogDataset(
        @NotBlank String version,
        @NotEmpty List<@Valid CatalogSpecializationData> specializations,
        @NotEmpty List<@Valid CatalogUnitData> units
) {

    public CatalogDataset {
        specializations = specializations == null
                ? null
                : List.copyOf(specializations);
        units = units == null ? null : List.copyOf(units);
    }
}
