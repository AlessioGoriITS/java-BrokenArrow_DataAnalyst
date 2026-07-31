package it.alessiogori.battledebrief.integration.barmory;

import com.fasterxml.jackson.databind.JsonNode;
import it.alessiogori.battledebrief.common.exception.ExternalProviderException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class BarmoryRestClient implements BarmoryGateway {

    private final RestClient client;
    private final BarmoryProperties properties;
    private final Clock clock;
    private final String clientId = UUID.randomUUID().toString();
    private volatile Attestation attestation;

    public BarmoryRestClient(
            RestClient.Builder builder,
            BarmoryProperties properties,
            Clock clock
    ) {
        this.client = builder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public JsonNode findCommander(String steamId) {
        return get("/stb/commander/{steamId}/steam", steamId);
    }

    @Override
    public JsonNode findCommanderStats(String steamId) {
        return get("/stb/commander/{steamId}/stats", steamId);
    }

    @Override
    public List<Long> findMatchIds(long commanderId, String week) {
        JsonNode response = get(
                "/stb/commander/{commanderId}/matches?time={week}",
                commanderId,
                week
        );
        List<Long> ids = new ArrayList<>();
        response.forEach(node -> ids.add(node.asLong()));
        return ids;
    }

    @Override
    public JsonNode findMatch(long matchId) {
        return get("/stb/match/{matchId}", matchId);
    }

    private JsonNode get(String uri, Object... variables) {
        try {
            return client.get()
                    .uri(uri, variables)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        headers.set("X-Barmory-ID", clientId);
                        headers.set("X-Barmory-Version", properties.version());
                        headers.set("X-Barmory-Attest", attestationToken());
                        headers.set("X-Type", "stb");
                    })
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new ExternalProviderException(
                    "BArmory is temporarily unavailable",
                    exception
            );
        }
    }

    private String attestationToken() {
        Attestation current = attestation;
        if (current == null || current.expiresAt().minusSeconds(15)
                .isBefore(clock.instant())) {
            synchronized (this) {
                current = attestation;
                if (current == null || current.expiresAt().minusSeconds(15)
                        .isBefore(clock.instant())) {
                    attestation = requestAttestation();
                }
                current = attestation;
            }
        }
        return current.token();
    }

    private Attestation requestAttestation() {
        try {
            JsonNode response = client.post()
                    .uri("/pulse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        headers.set("X-Barmory-ID", clientId);
                        headers.set("X-Barmory-Version", properties.version());
                        headers.set("X-Type", "NONE");
                    })
                    .body("{}")
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || response.path("token").isMissingNode()) {
                throw new ExternalProviderException(
                        "BArmory returned an invalid attestation"
                );
            }
            return new Attestation(
                    response.path("token").asText(),
                    Instant.ofEpochSecond(response.path("expiresAt").asLong())
            );
        } catch (RestClientException exception) {
            throw new ExternalProviderException(
                    "BArmory authentication is temporarily unavailable",
                    exception
            );
        }
    }

    private record Attestation(String token, Instant expiresAt) {
    }
}
