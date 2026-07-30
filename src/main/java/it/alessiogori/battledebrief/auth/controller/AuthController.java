package it.alessiogori.battledebrief.auth.controller;

import it.alessiogori.battledebrief.auth.dto.RegisterRequest;
import it.alessiogori.battledebrief.auth.dto.RegisterResponse;
import it.alessiogori.battledebrief.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisterResponse response = authService.register(request);

        return ResponseEntity
                .created(URI.create("/api/users/" + response.id()))
                .body(response);
    }
}
