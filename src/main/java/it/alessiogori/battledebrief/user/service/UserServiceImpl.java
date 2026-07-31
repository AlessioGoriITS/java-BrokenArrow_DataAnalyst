package it.alessiogori.battledebrief.user.service;

import it.alessiogori.battledebrief.common.dto.PageResponse;
import it.alessiogori.battledebrief.common.exception.DuplicateResourceException;
import it.alessiogori.battledebrief.common.exception.ResourceNotFoundException;
import it.alessiogori.battledebrief.integration.barmory.SteamPlayerService;
import it.alessiogori.battledebrief.integration.barmory.dto.SteamPlayerResponse;
import it.alessiogori.battledebrief.player.entity.PlayerProfile;
import it.alessiogori.battledebrief.player.repository.PlayerProfileRepository;
import it.alessiogori.battledebrief.user.dto.LinkSteamProfileRequest;
import it.alessiogori.battledebrief.user.dto.UpdateEnabledRequest;
import it.alessiogori.battledebrief.user.dto.UpdateRoleRequest;
import it.alessiogori.battledebrief.user.dto.UpdateUserRequest;
import it.alessiogori.battledebrief.user.dto.UserResponse;
import it.alessiogori.battledebrief.user.entity.User;
import it.alessiogori.battledebrief.user.mapper.UserMapper;
import it.alessiogori.battledebrief.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.time.Clock;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PlayerProfileRepository playerProfileRepository;
    private final SteamPlayerService steamPlayerService;
    private final Clock clock;

    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            PlayerProfileRepository playerProfileRepository,
            SteamPlayerService steamPlayerService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.playerProfileRepository = playerProfileRepository;
        this.steamPlayerService = steamPlayerService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UserResponse linkSteamProfile(
            Long userId,
            LinkSteamProfileRequest request
    ) {
        User user = requireUser(userId);
        String steamId = request.steamId().trim();

        playerProfileRepository.findBySteamId(steamId)
                .filter(profile -> !Objects.equals(
                        profile.getUser().getId(),
                        userId
                ))
                .ifPresent(profile -> {
                    throw new DuplicateResourceException(
                            "Steam ID is already linked to another account"
                    );
                });

        SteamPlayerResponse remote = steamPlayerService.findBySteamId(
                steamId,
                2,
                1
        );
        PlayerProfile profile = user.getPlayerProfile();
        if (profile == null) {
            profile = new PlayerProfile(remote.displayName());
            user.linkPlayerProfile(profile);
        }
        profile.updateExternalIdentity(
                remote.displayName(),
                steamId,
                Long.toString(remote.commanderId()),
                null
        );
        if (remote.currentRating() != null) {
            int currentElo = remote.currentRating().intValue();
            int peakElo = profile.getPeakElo() == null
                    ? currentElo
                    : Math.max(profile.getPeakElo(), currentElo);
            profile.updateElo(currentElo, peakElo);
        }
        profile.markSynchronized(clock.instant());

        try {
            return userMapper.toResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException(
                    "Steam ID is already linked to another account"
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long userId) {
        return userMapper.toResponse(requireUser(userId));
    }

    @Override
    @Transactional
    public UserResponse update(Long userId, UpdateUserRequest request) {
        User user = requireUser(userId);
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        userRepository.findByEmailIgnoreCase(email)
                .filter(existing -> !Objects.equals(existing.getId(), userId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Email already exists");
                });

        user.changeEmail(email);

        try {
            return userMapper.toResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException("Email already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findAll(Pageable pageable) {
        return PageResponse.from(
                userRepository.findAll(pageable).map(userMapper::toResponse)
        );
    }

    @Override
    @Transactional
    public UserResponse changeRole(Long userId, UpdateRoleRequest request) {
        User user = requireUser(userId);
        user.changeRole(request.role());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse changeEnabled(
            Long userId,
            UpdateEnabledRequest request
    ) {
        User user = requireUser(userId);
        user.setEnabled(request.enabled());
        return userMapper.toResponse(userRepository.save(user));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"
                ));
    }
}
