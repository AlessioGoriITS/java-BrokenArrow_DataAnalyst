package it.alessiogori.battledebrief.integration.barmory;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SteamProviderMetricsTests {

    @Test
    void recordsProviderDurationAndDiscardReasons() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SteamProviderMetrics metrics = new SteamProviderMetrics(registry);

        assertThat(registry.get("battle.debrief.steam.matches.discarded")
                .tags(
                        "provider", "BATTLEGROUP",
                        "reason", "telemetry_unavailable"
                )
                .counter().count()).isZero();

        metrics.recordLookup("BATTLEGROUP", Duration.ofMillis(125), true);
        metrics.recordDiscardedMatch(
                "BATTLEGROUP",
                "telemetry_unavailable"
        );
        metrics.recordInvalidField("BATTLEGROUP", "updateDate");

        assertThat(registry.get("battle.debrief.steam.lookups")
                .tags("provider", "BATTLEGROUP", "outcome", "success")
                .counter().count()).isEqualTo(1);
        assertThat(registry.get("battle.debrief.steam.lookup.duration")
                .tags("provider", "BATTLEGROUP", "outcome", "success")
                .timer().totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
                .isEqualTo(125);
        assertThat(registry.get("battle.debrief.steam.matches.discarded")
                .tags(
                        "provider", "BATTLEGROUP",
                        "reason", "telemetry_unavailable"
                )
                .counter().count()).isEqualTo(1);
        assertThat(registry.get("battle.debrief.steam.fields.invalid")
                .tags("provider", "BATTLEGROUP", "field", "updateDate")
                .counter().count()).isEqualTo(1);
    }
}
