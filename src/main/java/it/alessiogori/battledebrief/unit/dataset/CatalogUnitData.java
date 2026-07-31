package it.alessiogori.battledebrief.unit.dataset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CatalogUnitData(
        @NotBlank @Size(max = 100) String externalUnitId,
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 100) String faction,
        @NotBlank @Size(max = 100) String category,
        @NotNull @PositiveOrZero Integer baseCost,
        @Size(max = 4000) String description,
        @PositiveOrZero Integer hitPoints,
        @PositiveOrZero BigDecimal speed,
        @Size(max = 100) String armor,
        @Size(max = 200) String mainWeapon,
        @Size(max = 2048) String imageUrl,
        @NotNull List<@NotBlank String> specializations
) {

    public CatalogUnitData {
        specializations = specializations == null
                ? null
                : List.copyOf(specializations);
    }
}
