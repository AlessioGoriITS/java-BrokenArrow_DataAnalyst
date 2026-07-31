package it.alessiogori.battledebrief.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LinkSteamProfileRequest(
        @NotBlank
        @Pattern(regexp = "\\d{17}", message = "must be a 17 digit Steam ID")
        String steamId
) {
}
