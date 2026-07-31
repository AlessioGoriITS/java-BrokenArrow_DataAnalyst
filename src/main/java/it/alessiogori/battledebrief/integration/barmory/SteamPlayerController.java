package it.alessiogori.battledebrief.integration.barmory;

import it.alessiogori.battledebrief.integration.barmory.dto.SteamPlayerResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/steam/players")
public class SteamPlayerController {

    private final SteamPlayerService steamPlayerService;

    public SteamPlayerController(SteamPlayerService steamPlayerService) {
        this.steamPlayerService = steamPlayerService;
    }

    @GetMapping("/{steamId}")
    public ResponseEntity<SteamPlayerResponse> find(
            @PathVariable
            @Pattern(regexp = "\\d{17}", message = "must be a 17 digit Steam ID")
            String steamId,
            @RequestParam(defaultValue = "8") @Min(1) @Max(26) int weeks,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit
    ) {
        return ResponseEntity.ok(
                steamPlayerService.findBySteamId(steamId, weeks, limit)
        );
    }
}
