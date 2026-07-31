package it.alessiogori.battledebrief.unit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpecializationRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String faction,
        @Size(max = 2000) String description
) {
}
