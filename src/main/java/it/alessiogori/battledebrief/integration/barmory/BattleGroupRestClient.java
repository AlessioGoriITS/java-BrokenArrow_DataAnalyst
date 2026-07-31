package it.alessiogori.battledebrief.integration.barmory;

import com.fasterxml.jackson.databind.JsonNode;
import it.alessiogori.battledebrief.common.exception.ExternalProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BattleGroupRestClient implements BattleGroupGateway {

    private final RestClient client;
    private final RestClient matchClient;

    public BattleGroupRestClient(
            RestClient.Builder builder,
            @Value("${integration.battlegroup.base-url}") String baseUrl,
            @Value("${integration.battlegroup.match-base-url}") String matchBaseUrl
    ) {
        this.client = builder.baseUrl(baseUrl).build();
        this.matchClient = builder.clone().baseUrl(matchBaseUrl).build();
    }

    @Override
    public JsonNode findPlayerStats(String steamId) {
        try {
            return client.get()
                    .uri("/stats/{steamId}", steamId)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new ExternalProviderException(
                    "Player statistics providers are temporarily unavailable",
                    exception
            );
        }
    }

    @Override
    public JsonNode findPlayer(String steamId) {
        try {
            return client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/players/search")
                            .queryParam("steam_id", steamId)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new ExternalProviderException(
                    "Player identity provider is temporarily unavailable",
                    exception
            );
        }
    }

    @Override
    public JsonNode findMatch(long matchId) {
        try {
            return matchClient.get()
                    .uri("/fight_{matchId}.json", matchId)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new ExternalProviderException(
                    "Match telemetry is temporarily unavailable",
                    exception
            );
        }
    }
}
