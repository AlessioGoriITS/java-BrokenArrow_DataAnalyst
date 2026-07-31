package it.alessiogori.battledebrief.analytics.controller;

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
import it.alessiogori.battledebrief.user.entity.User;
import it.alessiogori.battledebrief.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DatasetAnalyticsApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlayerProfileRepository playerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private GameMatchRepository matchRepository;

    private PlayerProfile firstPlayer;
    private PlayerProfile secondPlayer;
    private Unit abrams;

    @BeforeEach
    void prepareData() {
        User firstUser = userRepository.save(new User(
                "dataset-one",
                "dataset-one@example.com",
                "test-password-hash"
        ));
        User secondUser = userRepository.save(new User(
                "dataset-two",
                "dataset-two@example.com",
                "test-password-hash"
        ));
        firstPlayer = new PlayerProfile("Dataset One");
        firstPlayer.assignTo(firstUser);
        secondPlayer = new PlayerProfile("Dataset Two");
        secondPlayer.assignTo(secondUser);
        playerRepository.save(firstPlayer);
        playerRepository.save(secondPlayer);
        playerRepository.flush();
        abrams = unitRepository.saveAndFlush(new Unit(
                "dataset_abrams",
                "M1A1 Abrams",
                "USA",
                "TANK",
                250,
                "test-1"
        ));
    }

    @Test
    void unitAnalyticsAggregateTheLocalDatasetAndArePublic()
            throws Exception {
        persistMatch(
                firstPlayer,
                "dataset-match-001",
                true,
                unitPerformance(250, 2, 1, 600)
        );
        persistMatch(
                secondPlayer,
                "dataset-match-002",
                false,
                unitPerformance(300, 3, 0, 400)
        );
        persistMatch(
                firstPlayer,
                "dataset-match-without-units",
                true,
                null
        );

        mockMvc.perform(get("/api/analytics/units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].externalUnitId")
                        .value("dataset_abrams"))
                .andExpect(jsonPath("$[0].sampleMatches").value(2))
                .andExpect(jsonPath("$[0].samplePlayers").value(2))
                .andExpect(jsonPath("$[0].datasetMatches").value(3))
                .andExpect(jsonPath("$[0].spawnedCount").value(5))
                .andExpect(jsonPath("$[0].lostCount").value(1))
                .andExpect(jsonPath("$[0].destroyedValue").value(1000))
                .andExpect(jsonPath("$[0].deploymentCost").value(1400))
                .andExpect(jsonPath("$[0].lostValue").value(250))
                .andExpect(jsonPath("$[0].playRate.value").value(66.67))
                .andExpect(jsonPath("$[0].winRate.value").value(50.0))
                .andExpect(jsonPath("$[0].economicKd.value").value(4.0))
                .andExpect(jsonPath("$[0].deploymentEfficiency.value")
                        .value(0.7143))
                .andExpect(jsonPath("$[0].survivalRate.value").value(80.0));
    }

    @Test
    void unitAnalyticsDetailIsPublicAndMissingSamplesReturnNotFound()
            throws Exception {
        persistMatch(
                firstPlayer,
                "dataset-detail-match",
                true,
                unitPerformance(250, 2, 0, 500)
        );
        Unit unusedUnit = unitRepository.saveAndFlush(new Unit(
                "dataset_unused",
                "Unused Unit",
                "USA",
                "VEHICLE",
                100,
                "test-1"
        ));

        mockMvc.perform(get(
                                "/api/analytics/units/{unitId}",
                                abrams.getId()
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitId").value(abrams.getId()))
                .andExpect(jsonPath("$.sampleMatches").value(1))
                .andExpect(jsonPath("$.samplePlayers").value(1))
                .andExpect(jsonPath("$.datasetMatches").value(1))
                .andExpect(jsonPath("$.playRate.value").value(100.0))
                .andExpect(jsonPath("$.winRate.value").value(100.0))
                .andExpect(jsonPath("$.economicKd.status")
                        .value("NO_LOSSES"));

        mockMvc.perform(get(
                                "/api/analytics/units/{unitId}",
                                unusedUnit.getId()
                        ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("Unit analytics not found in dataset"));
    }

    private UnitMatchPerformance unitPerformance(
            int unitCost,
            int spawned,
            int lost,
            long destroyedValue
    ) {
        UnitMatchPerformance performance = new UnitMatchPerformance(
                abrams,
                unitCost,
                spawned,
                lost
        );
        performance.updateMetrics(1, destroyedValue, 100L, 50L, 10L);
        return performance;
    }

    private void persistMatch(
            PlayerProfile player,
            String externalId,
            boolean won,
            UnitMatchPerformance unitPerformance
    ) {
        GameMatch match = new GameMatch(
                externalId,
                "River Crossing",
                "5V5",
                Instant.parse("2026-07-30T18:00:00Z"),
                1_800,
                TeamSide.TEAM_ONE,
                MatchSource.JSON_IMPORT
        );
        MatchPerformance performance = new MatchPerformance(
                player,
                TeamSide.TEAM_ONE,
                won
        );
        if (unitPerformance != null) {
            performance.addUnitPerformance(unitPerformance);
        }
        match.addPerformance(performance);
        matchRepository.saveAndFlush(match);
    }
}
