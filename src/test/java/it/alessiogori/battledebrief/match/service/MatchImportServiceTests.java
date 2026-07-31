package it.alessiogori.battledebrief.match.service;

import it.alessiogori.battledebrief.common.exception.ImportValidationException;
import it.alessiogori.battledebrief.match.dto.ImportedMatchRequest;
import it.alessiogori.battledebrief.match.dto.MatchImportRequest;
import it.alessiogori.battledebrief.match.dto.MatchImportResponse;
import it.alessiogori.battledebrief.match.dto.MatchPerformanceImportRequest;
import it.alessiogori.battledebrief.match.dto.UnitPerformanceImportRequest;
import it.alessiogori.battledebrief.match.entity.TeamSide;
import it.alessiogori.battledebrief.match.repository.GameMatchRepository;
import it.alessiogori.battledebrief.match.repository.MatchPerformanceRepository;
import it.alessiogori.battledebrief.match.repository.UnitMatchPerformanceRepository;
import it.alessiogori.battledebrief.player.entity.PlayerProfile;
import it.alessiogori.battledebrief.player.repository.PlayerProfileRepository;
import it.alessiogori.battledebrief.unit.entity.Unit;
import it.alessiogori.battledebrief.unit.repository.UnitRepository;
import it.alessiogori.battledebrief.user.entity.User;
import it.alessiogori.battledebrief.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class MatchImportServiceTests {

    @Autowired
    private MatchImportService importService;

    @Autowired
    private GameMatchRepository gameMatchRepository;

    @Autowired
    private MatchPerformanceRepository performanceRepository;

    @Autowired
    private UnitMatchPerformanceRepository unitPerformanceRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private UserRepository userRepository;

    private PlayerProfile player;

    @BeforeEach
    void prepareReferences() {
        clearDatabase();

        User user = userRepository.saveAndFlush(new User(
                "import-user",
                "import@example.com",
                "encoded-password"
        ));
        player = new PlayerProfile("Import Player");
        player.assignTo(user);
        player = playerProfileRepository.saveAndFlush(player);
        unitRepository.saveAndFlush(new Unit(
                "usa_m1a1_abrams",
                "M1A1 Abrams",
                "USA",
                "TANK",
                240,
                "test-1"
        ));
    }

    @AfterEach
    void removeReferences() {
        clearDatabase();
    }

    private void clearDatabase() {
        gameMatchRepository.deleteAll();
        unitRepository.deleteAll();
        playerProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void importsCompleteBatchAndPersistsNestedPerformances() {
        MatchImportResponse response = importService.importMatches(request(
                match("import-001", "usa_m1a1_abrams", 1),
                match("import-002", "usa_m1a1_abrams", 0)
        ));

        assertThat(response.importedCount()).isEqualTo(2);
        assertThat(response.skippedCount()).isZero();
        assertThat(response.importedMatchIds()).hasSize(2);
        assertThat(gameMatchRepository.count()).isEqualTo(2);
        assertThat(performanceRepository.count()).isEqualTo(2);
        assertThat(unitPerformanceRepository.count()).isEqualTo(2);

        var saved = gameMatchRepository.findByExternalMatchId("import-001")
                .orElseThrow();
        var performance = performanceRepository
                .findByGameMatchIdAndPlayerProfileId(saved.getId(), player.getId())
                .orElseThrow();
        assertThat(performance.isWon()).isTrue();
        assertThat(performance.getNewRating()).isEqualTo(1525);
        assertThat(unitPerformanceRepository
                .findAllByMatchPerformanceId(performance.getId()))
                .singleElement()
                .satisfies(unit -> {
                    assertThat(unit.getSpawnedCount()).isEqualTo(4);
                    assertThat(unit.getLostCount()).isEqualTo(1);
                    assertThat(unit.getDestroyedValue()).isEqualTo(900L);
                });
    }

    @Test
    void importingExistingMatchesReturnsSkippedWithoutDuplicates() {
        MatchImportRequest request = request(
                match("repeat-001", "usa_m1a1_abrams", 1)
        );
        importService.importMatches(request);

        MatchImportResponse repeated = importService.importMatches(request);

        assertThat(repeated.importedCount()).isZero();
        assertThat(repeated.skippedCount()).isEqualTo(1);
        assertThat(repeated.skippedExternalMatchIds())
                .containsExactly("repeat-001");
        assertThat(gameMatchRepository.count()).isEqualTo(1);
    }

    @Test
    void unknownUnitRollsBackEntireBatch() {
        MatchImportRequest request = request(
                match("rollback-valid", "usa_m1a1_abrams", 1),
                match("rollback-invalid", "missing-unit", 0)
        );

        assertThatThrownBy(() -> importService.importMatches(request))
                .isInstanceOf(ImportValidationException.class)
                .hasMessageContaining("Unknown unit externalUnitId");

        assertThat(gameMatchRepository.count()).isZero();
        assertThat(performanceRepository.count()).isZero();
        assertThat(unitPerformanceRepository.count()).isZero();
    }

    @Test
    void rejectsImpossibleLossesAndInconsistentWinner() {
        assertThatThrownBy(() -> importService.importMatches(request(
                match("invalid-losses", "usa_m1a1_abrams", 5)
        )))
                .isInstanceOf(ImportValidationException.class)
                .hasMessageContaining("lostCount cannot exceed spawnedCount");

        ImportedMatchRequest inconsistent = match(
                "invalid-winner",
                "usa_m1a1_abrams",
                1,
                TeamSide.TEAM_TWO,
                true
        );
        assertThatThrownBy(() -> importService.importMatches(request(inconsistent)))
                .isInstanceOf(ImportValidationException.class)
                .hasMessageContaining("result is inconsistent");

        assertThat(gameMatchRepository.count()).isZero();
    }

    @Test
    void rejectsDuplicateMatchIdsInsidePayload() {
        assertThatThrownBy(() -> importService.importMatches(request(
                match("same-id", "usa_m1a1_abrams", 1),
                match("same-id", "usa_m1a1_abrams", 0)
        )))
                .isInstanceOf(ImportValidationException.class)
                .hasMessageContaining("Duplicate externalMatchId");

        assertThat(gameMatchRepository.count()).isZero();
    }

    private MatchImportRequest request(ImportedMatchRequest... matches) {
        return new MatchImportRequest(player.getId(), List.of(matches));
    }

    private ImportedMatchRequest match(
            String externalId,
            String externalUnitId,
            int lostCount
    ) {
        return match(
                externalId,
                externalUnitId,
                lostCount,
                TeamSide.TEAM_ONE,
                true
        );
    }

    private ImportedMatchRequest match(
            String externalId,
            String externalUnitId,
            int lostCount,
            TeamSide team,
            boolean won
    ) {
        UnitPerformanceImportRequest unit = new UnitPerformanceImportRequest(
                externalUnitId,
                240,
                4,
                lostCount,
                3,
                900L,
                3_200L,
                1_100L,
                null
        );
        MatchPerformanceImportRequest performance =
                new MatchPerformanceImportRequest(
                        team,
                        won,
                        1500,
                        1525,
                        2_400L,
                        1_200L,
                        8_000L,
                        4_000L,
                        3,
                        960L,
                        0L,
                        250L,
                        List.of(unit)
                );
        return new ImportedMatchRequest(
                externalId,
                "River Crossing",
                "5V5",
                Instant.parse("2026-07-30T18:00:00Z"),
                1_800,
                TeamSide.TEAM_ONE,
                performance
        );
    }
}
