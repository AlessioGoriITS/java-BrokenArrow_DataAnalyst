package it.alessiogori.battledebrief.auth.service;

import it.alessiogori.battledebrief.auth.dto.LoginRequest;
import it.alessiogori.battledebrief.auth.dto.LoginResponse;
import it.alessiogori.battledebrief.auth.dto.RegisterRequest;
import it.alessiogori.battledebrief.auth.dto.RegisterResponse;
import it.alessiogori.battledebrief.auth.security.AuthenticatedUser;
import it.alessiogori.battledebrief.auth.security.IssuedJwt;
import it.alessiogori.battledebrief.auth.security.JwtService;
import it.alessiogori.battledebrief.common.exception.DuplicateResourceException;
import it.alessiogori.battledebrief.common.exception.InvalidCredentialsException;
import it.alessiogori.battledebrief.user.entity.Role;
import it.alessiogori.battledebrief.user.entity.User;
import it.alessiogori.battledebrief.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTests {

    private static final RegisterRequest REQUEST = new RegisterRequest(
            "Demo.User",
            "DEMO@Example.com",
            "Demo123!"
    );

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtService
        );
    }

    @Test
    void registersNormalizedUserWithEncodedPassword() {
        when(passwordEncoder.encode("Demo123!")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse response = authService.register(REQUEST);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getUsername()).isEqualTo("Demo.User");
        assertThat(savedUser.getEmail()).isEqualTo("demo@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(response.username()).isEqualTo("Demo.User");
        assertThat(response.email()).isEqualTo("demo@example.com");
        assertThat(response.role()).isEqualTo(Role.USER);
        assertThat(response.enabled()).isTrue();
    }

    @Test
    void rejectsExistingUsernameBeforeEncodingPassword() {
        when(userRepository.existsByUsernameIgnoreCase("Demo.User"))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.register(REQUEST))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username already exists");

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsExistingEmailBeforeEncodingPassword() {
        when(userRepository.existsByEmailIgnoreCase("demo@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.register(REQUEST))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already exists");

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void translatesDatabaseUniquenessRaceToDomainException() {
        when(passwordEncoder.encode("Demo123!")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> authService.register(REQUEST))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username or email already exists");
    }

    @Test
    void authenticatesCredentialsAndReturnsIssuedToken() {
        AuthenticatedUser principal = new AuthenticatedUser(
                42L,
                "demo",
                "encoded-password",
                Role.USER,
                true
        );
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                );
        Instant expiresAt = Instant.parse("2026-07-31T12:00:00Z");
        when(authenticationManager.authenticate(any()))
                .thenReturn(authentication);
        when(jwtService.issueToken(principal))
                .thenReturn(new IssuedJwt("signed-token", expiresAt));

        LoginResponse response = authService.login(
                new LoginRequest(" demo ", "Demo123!")
        );

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authCaptor.capture());
        assertThat(authCaptor.getValue().getPrincipal()).isEqualTo("demo");
        assertThat(authCaptor.getValue().getCredentials()).isEqualTo("Demo123!");
        assertThat(response.accessToken()).isEqualTo("signed-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
        assertThat(response.user().id()).isEqualTo(42L);
    }

    @Test
    void returnsControlledExceptionForInvalidCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("demo", "Wrong123!")
        ))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");

        verify(jwtService, never()).issueToken(any());
    }
}
