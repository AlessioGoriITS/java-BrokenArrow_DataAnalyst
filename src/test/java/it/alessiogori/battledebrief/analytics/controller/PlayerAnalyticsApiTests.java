package it.alessiogori.battledebrief.analytics.controller;

import it.alessiogori.battledebrief.auth.security.AuthenticatedUser;
import it.alessiogori.battledebrief.auth.security.JwtService;
import it.alessiogori.battledebrief.match.entity.GameMatch;
import it.alessiogori.battledebrief.match.entity.MatchPerformance;
import it.alessiogori.battledebrief.match.entity.MatchSource;
import it.alessiogori.battledebrief.match.entity.TeamSide;
import it.alessiogori.battledebrief.match.repository.GameMatchRepository;
import it.alessiogori.battledebrief.player.entity.PlayerProfile;
import it.alessiogori.battledebrief.player.repository.PlayerProfileRepository;
import it.alessiogori.battledebrief.user.entity.Role;
import it.alessiogori.battledebrief.user.entity.User;
import it.alessiogori.battledebrief.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlayerAnalyticsApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private GameMatchRepository gameMatchRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private PlayerProfile ownerProfile;
    private PlayerProfile otherProfile;
    private String ownerToken;
    private String otherToken;
    private String adminToken;

    @BeforeEach
    void preparePlayers() {
        String hash = passwordEncoder.encode("Demo123!");
        User owner = new User(
                "analytics-owner",
                "analytics-owner@example.com",
                hash
        );
        User other = new User(
                "analytics-other",
                "analytics-other@example.com",
                hash
        );
        User admin = new User(
                "analytics-admin",
                "analytics-admin@example.com",
                hash
        );
        admin.changeRole(Role.ADMIN);
        userRepository.saveAllAndFlush(List.of(owner, other, admin));

        ownerProfile = new PlayerProfile("Analytics Owner");
        ownerProfile.assignTo(owner);
        otherProfile = new PlayerProfile("Analytics Other");
        otherProfile.assignTo(other);
        playerProfileRepository.saveAllAndFlush(List.of(
                ownerProfile,
                otherProfile
        ));
        ownerToken = issueToken(owner);
        otherToken = issueToken(other);
        adminToken = issueToken(admin);
    }

    @Test
    void careerAnalysisUsesRatiosOfTotalsAndObservedElo() throws Exception {
        persistPerformance(
                ownerProfile,
                "analytics-001",
                Instant.parse("2026-07-28T18:00:00Z"),
                true,
                1500,
                1520,
                1_200L,
                0L,
                200L,
                0L,
                1_000L
        );
        persistPerformance(
                ownerProfile,
                "analytics-002",
                Instant.parse("2026-07-30T18:00:00Z"),
                false,
                1520,
                1505,
                600L,
                900L,
                300L,
                600L,
                600L
        );

        mockMvc.perform(get("/api/players/{id}/analysis", ownerProfile.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchCount").value(2))
                .andExpect(jsonPath("$.wins").value(1))
                .andExpect(jsonPath("$.losses").value(1))
                .andExpect(jsonPath("$.winRate.value").value(50.0))
                .andExpect(jsonPath("$.winRate.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.initialElo").value(1500))
                .andExpect(jsonPath("$.currentElo").value(1505))
                .andExpect(jsonPath("$.peakElo").value(1520))
                .andExpect(jsonPath("$.eloChange").value(5))
                .andExpect(jsonPath("$.totalDestroyedValue").value(1800))
                .andExpect(jsonPath("$.totalLostValue").value(900))
                .andExpect(jsonPath("$.averageDestroyedValue").value(900.0))
                .andExpect(jsonPath("$.averageLostValue").value(450.0))
                .andExpect(jsonPath("$.economicKd.value").value(2.0))
                .andExpect(jsonPath("$.deploymentEfficiency.value")
                        .value(1.125))
                .andExpect(jsonPath("$.damageRatio.value").value(0.8333));
    }

    @Test
    void zeroDenominatorsHaveNullValueAndSpecificStatus() throws Exception {
        persistPerformance(
                otherProfile,
                "analytics-zero",
                Instant.parse("2026-07-30T18:00:00Z"),
                true,
                1300,
                1310,
                500L,
                0L,
                100L,
                0L,
                0L
        );

        mockMvc.perform(get("/api/players/{id}/analysis", otherProfile.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.economicKd.value").value((Object) null))
                .andExpect(jsonPath("$.economicKd.status").value("NO_LOSSES"))
                .andExpect(jsonPath("$.deploymentEfficiency.value")
                        .value((Object) null))
                .andExpect(jsonPath("$.deploymentEfficiency.status")
                        .value("NO_DEPLOYMENTS"))
                .andExpect(jsonPath("$.damageRatio.value").value((Object) null))
                .andExpect(jsonPath("$.damageRatio.status")
                        .value("NO_DAMAGE_RECEIVED"));
    }

    @Test
    void emptyCareerAndAuthorizationAreHandled() throws Exception {
        mockMvc.perform(get("/api/players/{id}/analysis", ownerProfile.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/players/{id}/analysis", ownerProfile.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/players/{id}/analysis", ownerProfile.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchCount").value(0))
                .andExpect(jsonPath("$.winRate.value").value((Object) null))
                .andExpect(jsonPath("$.winRate.status").value("NO_MATCHES"))
                .andExpect(jsonPath("$.economicKd.status").value("NO_MATCHES"))
                .andExpect(jsonPath("$.averageDestroyedValue")
                        .value((Object) null));
    }

    private void persistPerformance(
            PlayerProfile player,
            String externalMatchId,
            Instant startedAt,
            boolean won,
            int oldRating,
            int newRating,
            long destroyed,
            long lost,
            long damageDealt,
            long damageReceived,
            long deployed
    ) {
        TeamSide winner = won ? TeamSide.TEAM_ONE : TeamSide.TEAM_TWO;
        GameMatch match = new GameMatch(
                externalMatchId,
                "River Crossing",
                "5V5",
                startedAt,
                1_800,
                winner,
                MatchSource.JSON_IMPORT
        );
        MatchPerformance performance = new MatchPerformance(
                player,
                TeamSide.TEAM_ONE,
                won
        );
        performance.updateMetrics(
                oldRating,
                newRating,
                destroyed,
                lost,
                damageDealt,
                damageReceived,
                1,
                deployed,
                0L,
                0L
        );
        match.addPerformance(performance);
        gameMatchRepository.saveAndFlush(match);
    }

    private String issueToken(User user) {
        return jwtService.issueToken(AuthenticatedUser.from(user)).value();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
