package it.alessiogori.battledebrief.auth.service;

import it.alessiogori.battledebrief.auth.dto.CurrentUserResponse;
import it.alessiogori.battledebrief.auth.dto.LoginRequest;
import it.alessiogori.battledebrief.auth.dto.LoginResponse;
import it.alessiogori.battledebrief.auth.dto.RegisterRequest;
import it.alessiogori.battledebrief.auth.dto.RegisterResponse;
import it.alessiogori.battledebrief.auth.security.AuthenticatedUser;
import it.alessiogori.battledebrief.auth.security.IssuedJwt;
import it.alessiogori.battledebrief.auth.security.JwtService;
import it.alessiogori.battledebrief.common.exception.DuplicateResourceException;
import it.alessiogori.battledebrief.common.exception.InvalidCredentialsException;
import it.alessiogori.battledebrief.user.entity.User;
import it.alessiogori.battledebrief.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username().trim(),
                            request.password()
                    )
            );
            AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
            IssuedJwt jwt = jwtService.issueToken(user);

            return new LoginResponse(
                    jwt.value(),
                    "Bearer",
                    jwt.expiresAt(),
                    CurrentUserResponse.from(user)
            );
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = new User(
                username,
                email,
                passwordEncoder.encode(request.password())
        );

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return toResponse(savedUser);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException(
                    "Username or email already exists"
            );
        }
    }

    private RegisterResponse toResponse(User user) {
        return new RegisterResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
