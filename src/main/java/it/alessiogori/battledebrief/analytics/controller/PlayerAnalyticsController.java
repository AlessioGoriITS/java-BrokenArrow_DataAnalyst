package it.alessiogori.battledebrief.analytics.controller;

import it.alessiogori.battledebrief.analytics.dto.PlayerAnalysisResponse;
import it.alessiogori.battledebrief.analytics.dto.PlayerTrendPointResponse;
import it.alessiogori.battledebrief.analytics.service.PlayerAnalyticsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/players/{playerId}/analysis")
@Validated
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

    @GetMapping("/trend")
    @PreAuthorize("@resourceAuthorization.canAccessPlayer("
            + "#playerId, authentication.principal.id, "
            + "authentication.principal.role)")
    public ResponseEntity<List<PlayerTrendPointResponse>> trend(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "10")
            @Min(1) @Max(100) int limit
    ) {
        return ResponseEntity.ok(playerAnalyticsService.trend(
                playerId,
                limit
        ));
    }
}
