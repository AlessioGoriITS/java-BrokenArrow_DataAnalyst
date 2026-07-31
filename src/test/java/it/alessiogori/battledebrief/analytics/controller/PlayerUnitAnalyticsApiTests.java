package it.alessiogori.battledebrief.analytics.controller;

import it.alessiogori.battledebrief.auth.security.AuthenticatedUser;
import it.alessiogori.battledebrief.auth.security.JwtService;
import it.alessiogori.battledebrief.match.entity.GameMatch;
import it.alessiogori.battledebrief.match.entity.MatchPerformance;
import it.alessiogori.battledebrief.match.entity.MatchSource;
import it.alessiogori.battledebrief.match.entity.TeamSide;
import it.alessiogori.battledebrief.match.entity.UnitMatchPerformance;
import it.alessiogori.battledebrief.match.repository.GameMatchRepository;
import it.alessiogori.battledebrief.player.entity.PlayerProfile;
import it.alessiogori.battledebrief.player.repository.PlayerProfileRepository;
import it.alessiogori.battledebrief.unit.entity.Unit;
import it.alessiogori.battledebrief.unit.repository.UnitRepository;
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
class PlayerUnitAnalyticsApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private GameMatchRepository gameMatchRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private PlayerProfile ownerProfile;
    private PlayerProfile otherProfile;
    private Unit abrams;
    private Unit humvee;
    private String ownerToken;
    private String otherToken;
    private String adminToken;

    @BeforeEach
    void prepareData() {
        String hash = passwordEncoder.encode("Demo123!");
        User owner = new User("unit-owner", "unit-owner@example.com", hash);
        User other = new User("unit-other", "unit-other@example.com", hash);
        User admin = new User("unit-admin", "unit-admin@example.com", hash);
        admin.changeRole(Role.ADMIN);
        userRepository.saveAllAndFlush(List.of(owner, other, admin));

        ownerProfile = new PlayerProfile("Unit Owner");
        ownerProfile.assignTo(owner);
        otherProfile = new PlayerProfile("Unit Other");
        otherProfile.assignTo(other);
        playerProfileRepository.saveAllAndFlush(List.of(
                ownerProfile,
                otherProfile
        ));

        abrams = unitRepository.saveAndFlush(new Unit(
                "analytics_abrams",
                "M1A1 Abrams",
                "USA",
                "TANK",
                250,
                "test-1"
        ));
        humvee = unitRepository.saveAndFlush(new Unit(
                "analytics_humvee",
                "M1025 Humvee",
                "USA",
                "VEHICLE",
                50,
                "test-1"
        ));
        ownerToken = issueToken(owner);
        otherToken = issueToken(other);
        adminToken = issueToken(admin);
    }

    @Test
    void aggregatesUnitEconomyAcrossMatchesAndOrdersByUsage()
            throws Exception {
        persistMatch(
                ownerProfile,
                "unit-match-001",
                Instant.parse("2026-07-28T18:00:00Z"),
                unitPerformance(abrams, 250, 4, 1, 3, 1_000, 1_000, 400, 30),
                unitPerformance(humvee, 50, 2, 1, 1, 100, 200, 100, 10)
        );
        persistMatch(
                ownerProfile,
                "unit-match-002",
                Instant.parse("2026-07-30T18:00:00Z"),
                unitPerformance(abrams, 300, 2, 0, 1, 200, 200, 100, 20)
        );

        mockMvc.perform(get("/api/players/{id}/units", ownerProfile.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].unitName").value("M1A1 Abrams"))
                .andExpect(jsonPath("$[0].sampleMatches").value(2))
                .andExpect(jsonPath("$[0].spawnedCount").value(6))
                .andExpect(jsonPath("$[0].lostCount").value(1))
                .andExpect(jsonPath("$[0].killsCount").value(4))
                .andExpect(jsonPath("$[0].deploymentCost").value(1600))
                .andExpect(jsonPath("$[0].lostValue").value(250))
                .andExpect(jsonPath("$[0].destroyedValue").value(1200))
                .andExpect(jsonPath("$[0].economicKd.value").value(4.8))
                .andExpect(jsonPath("$[0].deploymentEfficiency.value")
                        .value(0.75))
                .andExpect(jsonPath("$[0].survivalRate.value").value(83.33))
                .andExpect(jsonPath("$[0].damageRatio.value").value(2.4));

        mockMvc.perform(get(
                                "/api/players/{playerId}/units/{unitId}",
                                ownerProfile.getId(),
                                abrams.getId()
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalUnitId")
                        .value("analytics_abrams"))
                .andExpect(jsonPath("$.supplyConsumed").value(50));
    }

    @Test
    void endpointsEnforceOwnershipAndAllowAdmin() throws Exception {
        persistMatch(
                ownerProfile,
                "unit-auth-match",
                Instant.parse("2026-07-30T18:00:00Z"),
                unitPerformance(abrams, 250, 1, 0, 0, 0, 0, 0, 0)
        );

        mockMvc.perform(get("/api/players/{id}/units", ownerProfile.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/players/{id}/units", ownerProfile.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/players/{id}/units", ownerProfile.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get(
                                "/api/players/{playerId}/units/{unitId}",
                                ownerProfile.getId(),
                                humvee.getId()
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void zeroDeploymentsReturnExplicitUnavailableStatuses() throws Exception {
        persistMatch(
                otherProfile,
                "unit-zero-match",
                Instant.parse("2026-07-30T18:00:00Z"),
                unitPerformance(humvee, 50, 0, 0, 0, 100, 50, 0, 0)
        );

        mockMvc.perform(get(
                                "/api/players/{playerId}/units/{unitId}",
                                otherProfile.getId(),
                                humvee.getId()
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.economicKd.value").value((Object) null))
                .andExpect(jsonPath("$.economicKd.status").value("NO_LOSSES"))
                .andExpect(jsonPath("$.deploymentEfficiency.value")
                        .value((Object) null))
                .andExpect(jsonPath("$.deploymentEfficiency.status")
                        .value("NO_DEPLOYMENTS"))
                .andExpect(jsonPath("$.survivalRate.value").value((Object) null))
                .andExpect(jsonPath("$.survivalRate.status")
                        .value("NO_DEPLOYMENTS"))
                .andExpect(jsonPath("$.damageRatio.value").value((Object) null))
                .andExpect(jsonPath("$.damageRatio.status")
                        .value("NO_DAMAGE_RECEIVED"));
    }

    private UnitMatchPerformance unitPerformance(
            Unit unit,
            int cost,
            int spawned,
            int lost,
            int kills,
            long destroyed,
            long damageDealt,
            long damageReceived,
            long supply
    ) {
        UnitMatchPerformance performance = new UnitMatchPerformance(
                unit,
                cost,
                spawned,
                lost
        );
        performance.updateMetrics(
                kills,
                destroyed,
                damageDealt,
                damageReceived,
                supply
        );
        return performance;
    }

    private void persistMatch(
            PlayerProfile player,
            String externalId,
            Instant startedAt,
            UnitMatchPerformance... units
    ) {
        GameMatch match = new GameMatch(
                externalId,
                "River Crossing",
                "5V5",
                startedAt,
                1_800,
                TeamSide.TEAM_ONE,
                MatchSource.JSON_IMPORT
        );
        MatchPerformance performance = new MatchPerformance(
                player,
                TeamSide.TEAM_ONE,
                true
        );
        performance.updateMetrics(
                1500,
                1510,
                1_000L,
                500L,
                2_000L,
                1_000L,
                1,
                1_000L,
                0L,
                0L
        );
        for (UnitMatchPerformance unit : units) {
            performance.addUnitPerformance(unit);
        }
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
