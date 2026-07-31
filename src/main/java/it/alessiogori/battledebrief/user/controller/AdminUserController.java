package it.alessiogori.battledebrief.user.controller;

import it.alessiogori.battledebrief.common.dto.PageResponse;
import it.alessiogori.battledebrief.user.dto.UpdateEnabledRequest;
import it.alessiogori.battledebrief.user.dto.UpdateRoleRequest;
import it.alessiogori.battledebrief.user.dto.UserResponse;
import it.alessiogori.battledebrief.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> findAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(userService.findAll(pageable));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserResponse> changeRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        return ResponseEntity.ok(userService.changeRole(userId, request));
    }

    @PatchMapping("/{userId}/enabled")
    public ResponseEntity<UserResponse> changeEnabled(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateEnabledRequest request
    ) {
        return ResponseEntity.ok(userService.changeEnabled(userId, request));
    }
}
