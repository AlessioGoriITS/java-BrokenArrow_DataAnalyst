package it.alessiogori.battledebrief.integration.barmory;

import com.fasterxml.jackson.databind.JsonNode;

public interface BattleGroupGateway {

    JsonNode findPlayerStats(String steamId);

    JsonNode findPlayer(String steamId);

    JsonNode findMatch(long matchId);
}
