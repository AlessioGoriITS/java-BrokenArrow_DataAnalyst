package it.alessiogori.battledebrief.integration.barmory;

import it.alessiogori.battledebrief.integration.barmory.dto.SteamPlayerResponse;

public interface SteamPlayerService {

    SteamPlayerResponse findBySteamId(String steamId, int weeks, int limit);
}
