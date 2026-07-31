package it.alessiogori.battledebrief.integration.barmory;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ExternalHttpPropertiesTests {

    @Test
    void appliesSafeDefaults() {
        ExternalHttpProperties properties = new ExternalHttpProperties(
                null,
                null
        );

        assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void rejectsNonPositiveTimeouts() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new ExternalHttpProperties(Duration.ZERO, Duration.ofSeconds(1))
        );
        assertThatIllegalArgumentException().isThrownBy(() ->
                new ExternalHttpProperties(Duration.ofSeconds(1), Duration.ofSeconds(-1))
        );
    }
}
