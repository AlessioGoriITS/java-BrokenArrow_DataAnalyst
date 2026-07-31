package it.alessiogori.battledebrief.analytics.controller;

import it.alessiogori.battledebrief.analytics.dto.PlayerAnalysisResponse;
import it.alessiogori.battledebrief.analytics.service.PlayerAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players/{playerId}/analysis")
public class PlayerAnalyticsController {

    private final PlayerAnalyticsService playerAnalyticsService;

    public PlayerAnalyticsController(
            PlayerAnalyticsService playerAnalyticsService
    ) {
        this.playerAnalyticsService = playerAnalyticsService;
    }

    @GetMapping
    @PreAuthorize("@resourceAuthorization.canAccessPlayer("
            + "#playerId, authentication.principal.id, "
            + "authentication.principal.role)")
    public ResponseEntity<PlayerAnalysisResponse> analyze(
            @PathVariable Long playerId
    ) {
        return ResponseEntity.ok(playerAnalyticsService.analyze(playerId));
    }
}
