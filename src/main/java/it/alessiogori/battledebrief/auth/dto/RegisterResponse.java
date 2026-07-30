package it.alessiogori.battledebrief.auth.dto;

import it.alessiogori.battledebrief.user.entity.Role;

import java.time.Instant;

public record RegisterResponse(
        Long id,
        String username,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt
) {
}
