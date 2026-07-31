package it.alessiogori.battledebrief.user.mapper;

import it.alessiogori.battledebrief.player.entity.PlayerProfile;
import it.alessiogori.battledebrief.user.dto.PlayerProfileSummaryResponse;
import it.alessiogori.battledebrief.user.dto.UserResponse;
import it.alessiogori.battledebrief.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAuthProvider(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                toProfileSummary(user.getPlayerProfile())
        );
    }

    private PlayerProfileSummaryResponse toProfileSummary(PlayerProfile profile) {
        if (profile == null) {
            return null;
        }

        return new PlayerProfileSummaryResponse(
                profile.getId(),
                profile.getDisplayName(),
                profile.getSteamId(),
                profile.getExternalCommanderId()
        );
    }
}
