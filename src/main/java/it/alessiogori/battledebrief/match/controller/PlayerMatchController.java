package it.alessiogori.battledebrief.match.controller;

import it.alessiogori.battledebrief.common.dto.PageResponse;
import it.alessiogori.battledebrief.match.dto.MatchSearchCriteria;
import it.alessiogori.battledebrief.match.dto.PlayerMatchSummaryResponse;
import it.alessiogori.battledebrief.match.service.MatchQueryService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/players/{playerId}/matches")
public class PlayerMatchController {

    private final MatchQueryService matchQueryService;

    public PlayerMatchController(MatchQueryService matchQueryService) {
        this.matchQueryService = matchQueryService;
    }

    @GetMapping
    @PreAuthorize("@resourceAuthorization.canAccessPlayer("
            + "#playerId, authentication.principal.id, "
            + "authentication.principal.role)")
    public ResponseEntity<PageResponse<PlayerMatchSummaryResponse>> findAll(
            @PathVariable Long playerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Boolean won,
            @RequestParam(name = "map", required = false) String mapName,
            @RequestParam(required = false) Integer minElo,
            @PageableDefault(
                    size = 20,
                    sort = "startedAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        MatchSearchCriteria criteria = new MatchSearchCriteria(
                from,
                to,
                won,
                mapName,
                minElo
        );
        return ResponseEntity.ok(matchQueryService.findPlayerMatches(
                playerId,
                criteria,
                pageable
        ));
    }
}
