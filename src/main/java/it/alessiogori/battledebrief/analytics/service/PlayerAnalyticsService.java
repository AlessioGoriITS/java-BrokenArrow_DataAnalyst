package it.alessiogori.battledebrief.analytics.service;

import it.alessiogori.battledebrief.analytics.dto.PlayerAnalysisResponse;

public interface PlayerAnalyticsService {

    PlayerAnalysisResponse analyze(Long playerProfileId);
}
