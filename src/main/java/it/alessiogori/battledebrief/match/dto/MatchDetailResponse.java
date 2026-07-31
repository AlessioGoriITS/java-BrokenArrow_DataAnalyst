package it.alessiogori.battledebrief.match.dto;

import it.alessiogori.battledebrief.match.entity.MatchSource;
import it.alessiogori.battledebrief.match.entity.TeamSide;

import java.time.Instant;
import java.util.List;

public record MatchDetailResponse(
        Long id,
        String externalMatchId,
        String mapName,
        String gameMode,
        Instant startedAt,
        Integer durationSeconds,
        TeamSide winnerTeam,
        MatchSource source,
        Instant importedAt,
        List<MatchPerformanceResponse> performances
) {

    public MatchDetailResponse {
        performances = List.copyOf(performances);
    }
}
