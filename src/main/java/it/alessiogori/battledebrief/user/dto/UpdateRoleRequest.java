package it.alessiogori.battledebrief.user.dto;

import it.alessiogori.battledebrief.user.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull Role role
) {
}
