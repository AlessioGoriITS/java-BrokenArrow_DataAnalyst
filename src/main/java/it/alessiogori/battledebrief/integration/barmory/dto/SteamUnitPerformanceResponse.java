package it.alessiogori.battledebrief.integration.barmory.dto;

public record SteamUnitPerformanceResponse(
        long externalUnitId,
        String unitName,
        int deployed,
        int refunded,
        int kills,
        int damageDealt,
        int damageReceived
) {
}
