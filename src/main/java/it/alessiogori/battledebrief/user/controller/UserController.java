package it.alessiogori.battledebrief.user.controller;

import it.alessiogori.battledebrief.user.dto.UpdateUserRequest;
import it.alessiogori.battledebrief.user.dto.LinkSteamProfileRequest;
import it.alessiogori.battledebrief.user.dto.UserResponse;
import it.alessiogori.battledebrief.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    @PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getById(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(userService.getById(userId));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userService.update(userId, request));
    }

    @PutMapping("/{userId}/steam")
    @PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> linkSteamProfile(
            @PathVariable Long userId,
            @Valid @RequestBody LinkSteamProfileRequest request
    ) {
        return ResponseEntity.ok(
                userService.linkSteamProfile(userId, request)
        );
    }
}
