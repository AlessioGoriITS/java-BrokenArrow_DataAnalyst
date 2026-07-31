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

    public BattleGroupRestClient(
            RestClient.Builder builder,
            @Value("${integration.battlegroup.base-url}") String baseUrl
    ) {
        this.client = builder.baseUrl(baseUrl).build();
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
}
