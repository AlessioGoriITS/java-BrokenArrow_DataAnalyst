package it.alessiogori.battledebrief.user.service;

import it.alessiogori.battledebrief.common.dto.PageResponse;
import it.alessiogori.battledebrief.user.dto.UpdateEnabledRequest;
import it.alessiogori.battledebrief.user.dto.LinkSteamProfileRequest;
import it.alessiogori.battledebrief.user.dto.UpdateRoleRequest;
import it.alessiogori.battledebrief.user.dto.UpdateUserRequest;
import it.alessiogori.battledebrief.user.dto.UserResponse;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse getById(Long userId);

    UserResponse update(Long userId, UpdateUserRequest request);

    UserResponse linkSteamProfile(
            Long userId,
            LinkSteamProfileRequest request
    );

    PageResponse<UserResponse> findAll(Pageable pageable);

    UserResponse changeRole(Long userId, UpdateRoleRequest request);

    UserResponse changeEnabled(Long userId, UpdateEnabledRequest request);
}
