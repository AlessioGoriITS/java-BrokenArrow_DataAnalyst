package it.alessiogori.battledebrief.match.dto;

import it.alessiogori.battledebrief.match.entity.MatchSource;
import it.alessiogori.battledebrief.match.entity.TeamSide;

import java.time.Instant;

public record PlayerMatchSummaryResponse(
        Long id,
        String externalMatchId,
        String mapName,
        String gameMode,
        Instant startedAt,
        Integer durationSeconds,
        TeamSide winnerTeam,
        MatchSource source,
        TeamSide team,
        boolean won,
        Integer oldRating,
        Integer newRating,
        Long destructionScore,
        Long lossesScore
) {
}
