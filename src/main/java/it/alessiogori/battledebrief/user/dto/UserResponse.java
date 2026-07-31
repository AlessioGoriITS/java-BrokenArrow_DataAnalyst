package it.alessiogori.battledebrief.user.dto;

import it.alessiogori.battledebrief.user.entity.AuthProvider;
import it.alessiogori.battledebrief.user.entity.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String email,
        AuthProvider authProvider,
        Role role,
        boolean enabled,
        Instant createdAt,
        PlayerProfileSummaryResponse playerProfile
) {
}
