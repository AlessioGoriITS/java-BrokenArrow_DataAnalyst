package it.alessiogori.battledebrief.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(
                regexp = "^[A-Za-z0-9_.-]+$",
                message = "must contain only letters, numbers, dots, underscores or hyphens"
        )
        String username,

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)"
                        + "(?=.*[^A-Za-z\\d\\s])\\S+$",
                message = "must include uppercase, lowercase, number and special character"
        )
        String password
) {
}
