package it.alessiogori.battledebrief.auth.security;

import java.time.Instant;

public record IssuedJwt(
        String value,
        Instant expiresAt
) {
}
