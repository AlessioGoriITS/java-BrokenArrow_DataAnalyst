package it.alessiogori.battledebrief.auth.dto;

import it.alessiogori.battledebrief.auth.security.AuthenticatedUser;
import it.alessiogori.battledebrief.user.entity.Role;

public record CurrentUserResponse(
        Long id,
        String username,
        Role role
) {

    public static CurrentUserResponse from(AuthenticatedUser user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }
}
