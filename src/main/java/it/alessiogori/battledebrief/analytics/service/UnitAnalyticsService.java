package it.alessiogori.battledebrief.analytics.service;

import it.alessiogori.battledebrief.analytics.dto.PlayerUnitAnalysisResponse;
import it.alessiogori.battledebrief.analytics.dto.PlayerUnitMatchResponse;
import it.alessiogori.battledebrief.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UnitAnalyticsService {

    List<PlayerUnitAnalysisResponse> analyzeAll(Long playerProfileId);

    PlayerUnitAnalysisResponse analyzeUnit(
            Long playerProfileId,
            Long unitId
    );

    PageResponse<PlayerUnitMatchResponse> findUnitMatches(
            Long playerProfileId,
            Long unitId,
            Pageable pageable
    );
}
