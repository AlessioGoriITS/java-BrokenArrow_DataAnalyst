package it.alessiogori.battledebrief.match.repository;

import it.alessiogori.battledebrief.match.entity.GameMatch;
import it.alessiogori.battledebrief.match.entity.MatchPerformance;
import it.alessiogori.battledebrief.match.entity.MatchSource;
import it.alessiogori.battledebrief.match.entity.TeamSide;
import it.alessiogori.battledebrief.match.entity.UnitMatchPerformance;
import it.alessiogori.battledebrief.player.entity.PlayerProfile;
import it.alessiogori.battledebrief.player.repository.PlayerProfileRepository;
import it.alessiogori.battledebrief.unit.entity.Unit;
import it.alessiogori.battledebrief.unit.repository.UnitRepository;
import it.alessiogori.battledebrief.user.entity.User;
import it.alessiogori.battledebrief.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class MatchHistoryPersistenceTests {

    @Autowired
    private GameMatchRepository gameMatchRepository;

    @Autowired
    private MatchPerformanceRepository matchPerformanceRepository;

    @Autowired
    private UnitMatchPerformanceRepository unitPerformanceRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndQueriesCompleteMatchAggregate() {
        PlayerProfile player = createPlayer("alpha");
        Unit unit = createUnit("usa_test_abrams", "M1A1 Abrams");
        GameMatch match = match("match-001", TeamSide.TEAM_ONE);

        MatchPerformance performance = new MatchPerformance(
                player,
                TeamSide.TEAM_ONE,
                true
        );
        performance.updateMetrics(
                1500,
                1525,
                2_400L,
                1_200L,
                8_000L,
                4_000L,
                3,
                1_500L,
                100L,
                250L
        );
        UnitMatchPerformance unitPerformance = new UnitMatchPerformance(
                unit,
                240,
                4,
                1
        );
        unitPerformance.updateMetrics(3, 900L, 3_200L, 1_100L, null);
        performance.addUnitPerformance(unitPerformance);
        match.addPerformance(performance);

        gameMatchRepository.saveAndFlush(match);
        Long matchId = match.getId();
        Long performanceId = performance.getId();
        entityManager.clear();

        GameMatch saved = gameMatchRepository
                .findByExternalMatchId("match-001")
                .orElseThrow();
        assertThat(saved.getId()).isEqualTo(matchId);
        assertThat(saved.getImportedAt()).isNotNull();
        assertThat(saved.getPerformances()).hasSize(1);
        assertThat(saved.getPerformances().getFirst().getNewRating())
                .isEqualTo(1525);
        assertThat(saved.getPerformances().getFirst().getUnitPerformances())
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.getUnit().getExternalUnitId())
                            .isEqualTo("usa_test_abrams");
                    assertThat(result.getSpawnedCount()).isEqualTo(4);
                    assertThat(result.getDestroyedValue()).isEqualTo(900L);
                });

        assertThat(gameMatchRepository.findAllByPerformancesPlayerProfileId(
                player.getId(),
                PageRequest.of(0, 10)
        ).getTotalElements()).isEqualTo(1);
        assertThat(matchPerformanceRepository
                .findByGameMatchIdAndPlayerProfileId(matchId, player.getId()))
                .isPresent();
        assertThat(unitPerformanceRepository
                .findAllByMatchPerformanceId(performanceId))
                .hasSize(1);
        assertThat(unitPerformanceRepository
                .findAllByMatchPerformancePlayerProfileIdAndUnitId(
                        player.getId(),
                        unit.getId()
                )).hasSize(1);
    }

    @Test
    void rejectsDuplicateExternalMatchId() {
        gameMatchRepository.saveAndFlush(match("duplicate-match", null));

        assertThatThrownBy(() -> gameMatchRepository.saveAndFlush(
                match("duplicate-match", TeamSide.TEAM_TWO)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicatePlayerPerformanceWithinMatch() {
        PlayerProfile player = createPlayer("duplicate-player");
        GameMatch match = match("match-duplicate-player", TeamSide.TEAM_ONE);
        match.addPerformance(new MatchPerformance(
                player,
                TeamSide.TEAM_ONE,
                true
        ));
        match.addPerformance(new MatchPerformance(
                player,
                TeamSide.TEAM_ONE,
                true
        ));

        assertThatThrownBy(() -> gameMatchRepository.saveAndFlush(match))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private PlayerProfile createPlayer(String suffix) {
        User user = userRepository.save(new User(
                "user-" + suffix,
                suffix + "@match.test",
                "encoded-password"
        ));
        PlayerProfile profile = new PlayerProfile("Player " + suffix);
        profile.assignTo(user);
        return playerProfileRepository.saveAndFlush(profile);
    }

    private Unit createUnit(String externalId, String name) {
        return unitRepository.saveAndFlush(new Unit(
                externalId,
                name,
                "USA",
                "TANK",
                240,
                "test-1"
        ));
    }

    private GameMatch match(String externalId, TeamSide winnerTeam) {
        return new GameMatch(
                externalId,
                "River Crossing",
                "5V5",
                Instant.parse("2026-07-30T18:00:00Z"),
                1_800,
                winnerTeam,
                MatchSource.JSON_IMPORT
        );
    }
}
