package it.alessiogori.battledebrief.match.controller;

import it.alessiogori.battledebrief.match.dto.MatchDetailResponse;
import it.alessiogori.battledebrief.match.service.MatchQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchQueryService matchQueryService;

    public MatchController(MatchQueryService matchQueryService) {
        this.matchQueryService = matchQueryService;
    }

    @GetMapping("/{matchId}")
    @PreAuthorize("@resourceAuthorization.canAccessMatch("
            + "#matchId, authentication.principal.id, "
            + "authentication.principal.role)")
    public ResponseEntity<MatchDetailResponse> getById(
            @PathVariable Long matchId
    ) {
        return ResponseEntity.ok(matchQueryService.getById(matchId));
    }
}
