package it.alessiogori.battledebrief.analytics.controller;

import it.alessiogori.battledebrief.analytics.dto.PlayerUnitAnalysisResponse;
import it.alessiogori.battledebrief.analytics.dto.PlayerUnitMatchResponse;
import it.alessiogori.battledebrief.analytics.service.UnitAnalyticsService;
import it.alessiogori.battledebrief.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/players/{playerId}/units")
public class PlayerUnitAnalyticsController {

    private final UnitAnalyticsService unitAnalyticsService;

    public PlayerUnitAnalyticsController(
            UnitAnalyticsService unitAnalyticsService
    ) {
        this.unitAnalyticsService = unitAnalyticsService;
    }

    @GetMapping
    @PreAuthorize("@resourceAuthorization.canAccessPlayer("
            + "#playerId, authentication.principal.id, "
            + "authentication.principal.role)")
    public ResponseEntity<List<PlayerUnitAnalysisResponse>> findAll(
            @PathVariable Long playerId
    ) {
        return ResponseEntity.ok(unitAnalyticsService.analyzeAll(playerId));
    }

    @GetMapping("/{unitId}")
    @PreAuthorize("@resourceAuthorization.canAccessPlayer("
            + "#playerId, authentication.principal.id, "
            + "authentication.principal.role)")
    public ResponseEntity<PlayerUnitAnalysisResponse> getById(
            @PathVariable Long playerId,
            @PathVariable Long unitId
    ) {
        return ResponseEntity.ok(unitAnalyticsService.analyzeUnit(
                playerId,
                unitId
        ));
    }

    @GetMapping("/{unitId}/matches")
    @PreAuthorize("@resourceAuthorization.canAccessPlayer("
            + "#playerId, authentication.principal.id, "
            + "authentication.principal.role)")
    public ResponseEntity<PageResponse<PlayerUnitMatchResponse>> findMatches(
            @PathVariable Long playerId,
            @PathVariable Long unitId,
            @PageableDefault(
                    size = 20,
                    sort = "matchPerformance.gameMatch.startedAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(unitAnalyticsService.findUnitMatches(
                playerId,
                unitId,
                pageable
        ));
    }
}
