package it.alessiogori.battledebrief.auth.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        @NotBlank String issuer,
        @NotNull Duration expiration,
        @NotBlank String secret
) {

    public JwtProperties {
        if (expiration != null && expiration.compareTo(Duration.ofMinutes(1)) < 0) {
            throw new IllegalArgumentException(
                    "JWT expiration must be at least one minute"
            );
        }
    }
}
