package it.alessiogori.battledebrief.integration.barmory;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface BarmoryGateway {

    JsonNode findCommander(String steamId);

    JsonNode findCommanderStats(String steamId);

    List<Long> findMatchIds(long commanderId, String week);

    JsonNode findMatch(long matchId);
}
