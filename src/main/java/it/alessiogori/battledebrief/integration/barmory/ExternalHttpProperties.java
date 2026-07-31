package it.alessiogori.battledebrief.integration.barmory;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "integration.http")
public record ExternalHttpProperties(
        Duration connectTimeout,
        Duration readTimeout
) {

    public ExternalHttpProperties {
        connectTimeout = connectTimeout == null
                ? Duration.ofSeconds(3)
                : requirePositive(connectTimeout, "connect-timeout");
        readTimeout = readTimeout == null
                ? Duration.ofSeconds(10)
                : requirePositive(readTimeout, "read-timeout");
    }

    private static Duration requirePositive(Duration value, String name) {
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(
                    "integration.http." + name + " must be positive"
            );
        }
        return value;
    }
}
