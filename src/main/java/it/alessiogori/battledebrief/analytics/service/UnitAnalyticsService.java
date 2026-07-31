package it.alessiogori.battledebrief.analytics.service;

import it.alessiogori.battledebrief.analytics.dto.PlayerUnitAnalysisResponse;

import java.util.List;

public interface UnitAnalyticsService {

    List<PlayerUnitAnalysisResponse> analyzeAll(Long playerProfileId);

    PlayerUnitAnalysisResponse analyzeUnit(
            Long playerProfileId,
            Long unitId
    );
}
