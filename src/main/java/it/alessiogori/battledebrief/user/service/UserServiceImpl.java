package it.alessiogori.battledebrief.user.service;

import it.alessiogori.battledebrief.common.dto.PageResponse;
import it.alessiogori.battledebrief.common.exception.DuplicateResourceException;
import it.alessiogori.battledebrief.common.exception.ResourceNotFoundException;
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

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
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
