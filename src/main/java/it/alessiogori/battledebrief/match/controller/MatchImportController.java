package it.alessiogori.battledebrief.match.controller;

import it.alessiogori.battledebrief.match.dto.MatchImportRequest;
import it.alessiogori.battledebrief.match.dto.MatchImportResponse;
import it.alessiogori.battledebrief.match.service.MatchImportService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/matches/import")
public class MatchImportController {

    private final MatchImportService matchImportService;

    public MatchImportController(MatchImportService matchImportService) {
        this.matchImportService = matchImportService;
    }

    @PostMapping
    @PreAuthorize("@resourceAuthorization.canAccessPlayer("
            + "#request.playerProfileId, authentication.principal.id, "
            + "authentication.principal.role)")
    public ResponseEntity<MatchImportResponse> importMatches(
            @Valid @RequestBody MatchImportRequest request
    ) {
        MatchImportResponse response = matchImportService.importMatches(request);
        if (response.importedCount() == 0) {
            return ResponseEntity.ok(response);
        }

        URI playerHistory = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/players/{playerId}/matches")
                .buildAndExpand(request.playerProfileId())
                .toUri();
        return ResponseEntity.created(playerHistory).body(response);
    }
}
