package it.alessiogori.battledebrief.match.service;

import it.alessiogori.battledebrief.common.dto.PageResponse;
import it.alessiogori.battledebrief.match.dto.MatchDetailResponse;
import it.alessiogori.battledebrief.match.dto.MatchSearchCriteria;
import it.alessiogori.battledebrief.match.dto.PlayerMatchSummaryResponse;
import org.springframework.data.domain.Pageable;

public interface MatchQueryService {

    PageResponse<PlayerMatchSummaryResponse> findPlayerMatches(
            Long playerProfileId,
            MatchSearchCriteria criteria,
            Pageable pageable
    );

    MatchDetailResponse getById(Long matchId);
}
