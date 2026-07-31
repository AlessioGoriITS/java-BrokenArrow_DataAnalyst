package it.alessiogori.battledebrief.user.dto;

public record PlayerProfileSummaryResponse(
        Long id,
        String displayName,
        String steamId,
        String externalCommanderId
) {
}
