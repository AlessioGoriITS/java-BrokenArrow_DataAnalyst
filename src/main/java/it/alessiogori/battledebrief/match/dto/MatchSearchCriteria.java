package it.alessiogori.battledebrief.match.dto;

import java.time.Instant;

public record MatchSearchCriteria(
        Instant from,
        Instant to,
        Boolean won,
        String mapName,
        Integer minElo
) {
}
