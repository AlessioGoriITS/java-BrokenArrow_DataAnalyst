package it.alessiogori.battledebrief.analytics.service;

import it.alessiogori.battledebrief.analytics.dto.PlayerAnalysisResponse;
import it.alessiogori.battledebrief.analytics.dto.PlayerTrendPointResponse;

import java.util.List;

public interface PlayerAnalyticsService {

    PlayerAnalysisResponse analyze(Long playerProfileId);

    List<PlayerTrendPointResponse> trend(Long playerProfileId, int limit);
}
