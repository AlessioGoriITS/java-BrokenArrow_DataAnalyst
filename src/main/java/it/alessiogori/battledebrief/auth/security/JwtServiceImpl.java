package it.alessiogori.battledebrief.auth.security;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;
    private final JwtParser parser;

    public JwtServiceImpl(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(properties.secret())
        );
        this.parser = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    @Override
    public IssuedJwt issueToken(AuthenticatedUser user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.expiration());

        String token = Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();

        return new IssuedJwt(token, expiresAt);
    }

    @Override
    public String parseSubject(String token) {
        return parser
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
