package it.alessiogori.battledebrief.user.controller;

import it.alessiogori.battledebrief.auth.security.AuthenticatedUser;
import it.alessiogori.battledebrief.auth.security.JwtService;
import it.alessiogori.battledebrief.integration.barmory.SteamPlayerService;
import it.alessiogori.battledebrief.integration.barmory.dto.SteamPlayerResponse;
import it.alessiogori.battledebrief.user.entity.Role;
import it.alessiogori.battledebrief.user.entity.User;
import it.alessiogori.battledebrief.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserAuthorizationApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private SteamPlayerService steamPlayerService;

    private User owner;
    private User otherUser;
    private User admin;
    private String ownerToken;
    private String otherUserToken;
    private String adminToken;

    @BeforeEach
    void createUsers() {
        String passwordHash = passwordEncoder.encode("Demo123!");
        owner = new User("owner", "owner@example.com", passwordHash);
        otherUser = new User("other", "other@example.com", passwordHash);
        admin = new User("admin", "admin@example.com", passwordHash);
        admin.changeRole(Role.ADMIN);
        userRepository.saveAllAndFlush(List.of(owner, otherUser, admin));

        ownerToken = issueToken(owner);
        otherUserToken = issueToken(otherUser);
        adminToken = issueToken(admin);
    }

    @Test
    void ownerCanReadOwnAccountWithoutPasswordData() throws Exception {
        mockMvc.perform(get("/api/users/{id}", owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(owner.getId()))
                .andExpect(jsonPath("$.username").value("owner"))
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void userCannotReadAnotherAccount() throws Exception {
        mockMvc.perform(get("/api/users/{id}", otherUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN_OPERATION"))
                .andExpect(jsonPath("$.path").value(
                        "/api/users/" + otherUser.getId()
                ));
    }

    @Test
    void adminCanReadAnyAccountAndUsePaginatedList() throws Exception {
        mockMvc.perform(get("/api/users/{id}", otherUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("other"));

        mockMvc.perform(get("/api/admin/users")
                        .queryParam("page", "0")
                        .queryParam("size", "2")
                        .queryParam("sort", "username,asc")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void regularUserCannotUseAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN_OPERATION"));
    }

    @Test
    void ownerCanUpdateEmailAndServiceNormalizesIt() throws Exception {
        mockMvc.perform(put("/api/users/{id}", owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "Updated@Example.COM"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"));

        assertThat(userRepository.findById(owner.getId()).orElseThrow().getEmail())
                .isEqualTo("updated@example.com");
    }

    @Test
    void ownerCannotReuseAnotherUsersEmail() throws Exception {
        mockMvc.perform(put("/api/users/{id}", owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "OTHER@example.com"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void ownerCanLinkAValidatedSteamProfile() throws Exception {
        String steamId = "76561198776876377";
        when(steamPlayerService.findBySteamId(steamId, 2, 1))
                .thenReturn(new SteamPlayerResponse(
                        steamId,
                        48123L,
                        "Test Commander",
                        24,
                        new BigDecimal("1712.8"),
                        250,
                        null,
                        List.of(),
                        List.of(),
                        "BATTLEGROUP",
                        Instant.parse("2026-07-31T12:00:00Z")
                ));

        mockMvc.perform(put("/api/users/{id}/steam", owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "steamId": "76561198776876377"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerProfile.steamId").value(steamId))
                .andExpect(jsonPath("$.playerProfile.displayName")
                        .value("Test Commander"));

        mockMvc.perform(put("/api/users/{id}/steam", owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherUserToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "steamId": "76561198776876377"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanChangeRoleAndDisableAccountImmediately() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/role", owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(patch(
                                "/api/admin/users/{id}/enabled",
                                otherUser.getId()
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(get("/api/users/{id}", otherUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherUserToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    private String issueToken(User user) {
        return jwtService.issueToken(AuthenticatedUser.from(user)).value();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
