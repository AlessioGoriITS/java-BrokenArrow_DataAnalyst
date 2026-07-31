package it.alessiogori.battledebrief.auth.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import it.alessiogori.battledebrief.user.entity.Role;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceImplTests {

    private static final String SECRET =
            "YmF0dGxlLWRlYnJpZWYtdGVzdC1zZWNyZXQta2V5LTMyaGFzaGJ5dGVzLWxvbmc=";
    private static final Instant NOW = Instant.parse("2026-07-31T10:00:00Z");
    private static final JwtProperties PROPERTIES = new JwtProperties(
            "battle-debrief-test",
            Duration.ofHours(1),
            SECRET
    );

    @Test
    void issuesAndValidatesSignedToken() {
        JwtService jwtService = serviceAt(NOW);
        AuthenticatedUser user = new AuthenticatedUser(
                42L,
                "demo",
                "password-hash",
                Role.ADMIN,
                true
        );

        IssuedJwt jwt = jwtService.issueToken(user);

        assertThat(jwt.value()).hasSizeGreaterThan(100);
        assertThat(jwt.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
        assertThat(jwtService.parseSubject(jwt.value())).isEqualTo("demo");
        assertThat(jwt.value()).doesNotContain("password-hash");
    }

    @Test
    void rejectsModifiedToken() {
        JwtService jwtService = serviceAt(NOW);
        AuthenticatedUser user = new AuthenticatedUser(
                42L,
                "demo",
                "password-hash",
                Role.USER,
                true
        );
        String token = jwtService.issueToken(user).value();
        int payloadStart = token.indexOf('.') + 1;
        char originalCharacter = token.charAt(payloadStart);
        char replacementCharacter = originalCharacter == 'a' ? 'b' : 'a';
        String modifiedToken = token.substring(0, payloadStart)
                + replacementCharacter
                + token.substring(payloadStart + 1);

        assertThatThrownBy(() -> jwtService.parseSubject(modifiedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        AuthenticatedUser user = new AuthenticatedUser(
                42L,
                "demo",
                "password-hash",
                Role.USER,
                true
        );
        String token = serviceAt(NOW).issueToken(user).value();
        JwtService futureService = serviceAt(NOW.plus(Duration.ofHours(2)));

        assertThatThrownBy(() -> futureService.parseSubject(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsExpirationShorterThanOneMinute() {
        assertThatThrownBy(() -> new JwtProperties(
                "issuer",
                Duration.ofSeconds(59),
                SECRET
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT expiration must be at least one minute");
    }

    private JwtService serviceAt(Instant instant) {
        Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
        return new JwtServiceImpl(PROPERTIES, clock);
    }
}
