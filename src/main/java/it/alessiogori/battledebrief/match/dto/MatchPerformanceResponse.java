package it.alessiogori.battledebrief.match.dto;

import it.alessiogori.battledebrief.match.entity.TeamSide;

import java.util.List;

public record MatchPerformanceResponse(
        Long playerProfileId,
        String playerDisplayName,
        TeamSide team,
        boolean won,
        Integer oldRating,
        Integer newRating,
        Long destructionScore,
        Long lossesScore,
        Long damageDealt,
        Long damageReceived,
        Integer objectivesCaptured,
        Long spawnedUnitScore,
        Long refundedUnitScore,
        Long supplyConsumed,
        List<UnitMatchPerformanceResponse> units
) {

    public MatchPerformanceResponse {
        units = List.copyOf(units);
    }
}
