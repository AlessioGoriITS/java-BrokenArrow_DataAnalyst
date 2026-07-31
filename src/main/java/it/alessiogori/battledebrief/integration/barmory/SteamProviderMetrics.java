package it.alessiogori.battledebrief.integration.barmory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class SteamProviderMetrics {

    private final MeterRegistry meterRegistry;

    public SteamProviderMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        for (String provider : List.of("BARMORY", "BATTLEGROUP")) {
            discardedMatchCounter(
                    provider,
                    "telemetry_unavailable"
            );
        }
    }

    public void recordLookup(
            String provider,
            Duration duration,
            boolean successful
    ) {
        String outcome = successful ? "success" : "failure";
        Counter.builder("battle.debrief.steam.lookups")
                .description("Steam provider lookup attempts")
                .tag("provider", provider)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
        Timer.builder("battle.debrief.steam.lookup.duration")
                .description("Steam provider lookup duration")
                .tag("provider", provider)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(duration);
    }

    public void recordDiscardedMatch(String provider, String reason) {
        discardedMatchCounter(provider, reason).increment();
    }

    private Counter discardedMatchCounter(String provider, String reason) {
        return Counter.builder("battle.debrief.steam.matches.discarded")
                .description("Match payloads discarded during Steam lookups")
                .tag("provider", provider)
                .tag("reason", reason)
                .register(meterRegistry);
    }

    public void recordInvalidField(String provider, String field) {
        Counter.builder("battle.debrief.steam.fields.invalid")
                .description("Invalid provider fields ignored during parsing")
                .tag("provider", provider)
                .tag("field", field)
                .register(meterRegistry)
                .increment();
    }
}
