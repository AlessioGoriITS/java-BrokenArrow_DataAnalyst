package it.alessiogori.battledebrief.match.controller;

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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MatchHistoryApiTests {

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
    private String ownerToken;
    private String otherToken;
    private String adminToken;

    @BeforeEach
    void prepareUsersAndCatalog() {
        String passwordHash = passwordEncoder.encode("Demo123!");
        User owner = new User(
                "match-owner",
                "match-owner@example.com",
                passwordHash
        );
        User other = new User(
                "match-other",
                "match-other@example.com",
                passwordHash
        );
        User admin = new User(
                "match-admin",
                "match-admin@example.com",
                passwordHash
        );
        admin.changeRole(Role.ADMIN);
        userRepository.saveAllAndFlush(List.of(owner, other, admin));

        ownerProfile = new PlayerProfile("Match Owner");
        ownerProfile.assignTo(owner);
        otherProfile = new PlayerProfile("Match Other");
        otherProfile.assignTo(other);
        playerProfileRepository.saveAllAndFlush(List.of(
                ownerProfile,
                otherProfile
        ));

        abrams = unitRepository.saveAndFlush(new Unit(
                "api_abrams",
                "M1A1 Abrams",
                "USA",
                "TANK",
                240,
                "test-1"
        ));
        ownerToken = issueToken(owner);
        otherToken = issueToken(other);
        adminToken = issueToken(admin);
    }

    @Test
    void ownerCanImportWhileAnonymousAndOtherUserCannot() throws Exception {
        String request = importJson(ownerProfile.getId(), "api-import-001");

        mockMvc.perform(post("/api/matches/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/matches/import")
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN_OPERATION"));

        mockMvc.perform(post("/api/matches/import")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "http://localhost/api/players/"
                                + ownerProfile.getId() + "/matches"
                ))
                .andExpect(jsonPath("$.importedCount").value(1))
                .andExpect(jsonPath("$.skippedCount").value(0))
                .andExpect(jsonPath("$.importedMatchIds.length()").value(1));

        mockMvc.perform(post("/api/matches/import")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(0))
                .andExpect(jsonPath("$.skippedCount").value(1))
                .andExpect(jsonPath("$.skippedExternalMatchIds[0]")
                        .value("api-import-001"));
    }

    @Test
    void historyCombinesFiltersAndPaginatesForOwnerAndAdmin()
            throws Exception {
        persistMatch(
                "history-001",
                "River Crossing",
                Instant.parse("2026-07-30T18:00:00Z"),
                true,
                1525
        );
        persistMatch(
                "history-002",
                "River Crossing",
                Instant.parse("2026-07-29T18:00:00Z"),
                false,
                1490
        );
        persistMatch(
                "history-003",
                "Airport",
                Instant.parse("2026-07-28T18:00:00Z"),
                true,
                1550
        );

        mockMvc.perform(get("/api/players/{id}/matches", ownerProfile.getId())
                        .queryParam("from", "2026-07-29T00:00:00Z")
                        .queryParam("to", "2026-07-31T00:00:00Z")
                        .queryParam("won", "true")
                        .queryParam("map", "river")
                        .queryParam("minElo", "1500")
                        .queryParam("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].externalMatchId")
                        .value("history-001"))
                .andExpect(jsonPath("$.content[0].newRating").value(1525))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(1));

        mockMvc.perform(get("/api/players/{id}/matches", ownerProfile.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/players/{id}/matches", ownerProfile.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void detailIncludesUnitDataAndEnforcesMatchOwnership() throws Exception {
        GameMatch match = persistMatch(
                "detail-001",
                "River Crossing",
                Instant.parse("2026-07-30T18:00:00Z"),
                true,
                1525
        );

        mockMvc.perform(get("/api/matches/{id}", match.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalMatchId").value("detail-001"))
                .andExpect(jsonPath("$.performances[0].playerProfileId")
                        .value(ownerProfile.getId()))
                .andExpect(jsonPath("$.performances[0].units[0].externalUnitId")
                        .value("api_abrams"))
                .andExpect(jsonPath("$.performances[0].units[0].spawnedCount")
                        .value(4));

        mockMvc.perform(get("/api/matches/{id}", match.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/matches/{id}", match.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/matches/{id}", 999999L)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void invalidHistoryRangeAndInvalidImportReturnBadRequest()
            throws Exception {
        mockMvc.perform(get("/api/players/{id}/matches", ownerProfile.getId())
                        .queryParam("from", "2026-07-31T00:00:00Z")
                        .queryParam("to", "2026-07-01T00:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));

        mockMvc.perform(post("/api/matches/import")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerProfileId": %d,
                                  "matches": []
                                }
                                """.formatted(ownerProfile.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    private GameMatch persistMatch(
            String externalId,
            String map,
            Instant startedAt,
            boolean won,
            int newRating
    ) {
        TeamSide winner = won ? TeamSide.TEAM_ONE : TeamSide.TEAM_TWO;
        GameMatch match = new GameMatch(
                externalId,
                map,
                "5V5",
                startedAt,
                1_800,
                winner,
                MatchSource.JSON_IMPORT
        );
        MatchPerformance performance = new MatchPerformance(
                ownerProfile,
                TeamSide.TEAM_ONE,
                won
        );
        performance.updateMetrics(
                1500,
                newRating,
                2_400L,
                1_200L,
                8_000L,
                4_000L,
                3,
                960L,
                0L,
                250L
        );
        UnitMatchPerformance unit = new UnitMatchPerformance(
                abrams,
                240,
                4,
                1
        );
        unit.updateMetrics(3, 900L, 3_200L, 1_100L, null);
        performance.addUnitPerformance(unit);
        match.addPerformance(performance);
        return gameMatchRepository.saveAndFlush(match);
    }

    private String importJson(Long playerProfileId, String externalMatchId) {
        return """
                {
                  "playerProfileId": %d,
                  "matches": [{
                    "externalMatchId": "%s",
                    "mapName": "River Crossing",
                    "gameMode": "5V5",
                    "startedAt": "2026-07-30T18:00:00Z",
                    "durationSeconds": 1800,
                    "winnerTeam": "TEAM_ONE",
                    "performance": {
                      "team": "TEAM_ONE",
                      "won": true,
                      "oldRating": 1500,
                      "newRating": 1525,
                      "destructionScore": 2400,
                      "lossesScore": 1200,
                      "damageDealt": 8000,
                      "damageReceived": 4000,
                      "objectivesCaptured": 3,
                      "spawnedUnitScore": 960,
                      "refundedUnitScore": 0,
                      "supplyConsumed": 250,
                      "units": [{
                        "externalUnitId": "api_abrams",
                        "unitCost": 240,
                        "spawnedCount": 4,
                        "lostCount": 1,
                        "killsCount": 3,
                        "destroyedValue": 900,
                        "damageDealt": 3200,
                        "damageReceived": 1100,
                        "supplyConsumed": null
                      }]
                    }
                  }]
                }
                """.formatted(playerProfileId, externalMatchId);
    }

    private String issueToken(User user) {
        return jwtService.issueToken(AuthenticatedUser.from(user)).value();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
