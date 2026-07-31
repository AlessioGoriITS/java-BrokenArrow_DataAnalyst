package it.alessiogori.battledebrief;

import it.alessiogori.battledebrief.match.repository.GameMatchRepository;
import it.alessiogori.battledebrief.match.repository.MatchPerformanceRepository;
import it.alessiogori.battledebrief.match.repository.UnitMatchPerformanceRepository;
import it.alessiogori.battledebrief.player.repository.PlayerProfileRepository;
import it.alessiogori.battledebrief.user.entity.Role;
import it.alessiogori.battledebrief.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:schema_validation;"
                + "MODE=MySQL;DATABASE_TO_LOWER=TRUE;"
                + "INIT=RUNSCRIPT FROM 'database/schema.sql'",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never",
        "app.catalog.initialize=false"
})
@ActiveProfiles("test")
@Sql("file:database/demo-data.sql")
class DatabaseSchemaValidationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GameMatchRepository gameMatchRepository;

    @Autowired
    private MatchPerformanceRepository matchPerformanceRepository;

    @Autowired
    private UnitMatchPerformanceRepository unitMatchPerformanceRepository;

    @Test
    void schemaMatchesTheJpaModel() {
    }

    @Test
    void demoAccountsAreLinkedAndUseBcryptPasswords() {
        assertThat(userRepository.count()).isEqualTo(3);
        assertThat(playerProfileRepository.count()).isEqualTo(3);

        var admin = userRepository.findByUsernameIgnoreCase("admin")
                .orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(passwordEncoder.matches(
                "Admin123!",
                admin.getPasswordHash()
        )).isTrue();
    }

    @Test
    void demoTelemetryProvidesInitialFrontendAnalytics() {
        assertThat(gameMatchRepository.count()).isEqualTo(6);
        assertThat(matchPerformanceRepository.count()).isEqualTo(6);
        assertThat(unitMatchPerformanceRepository.count()).isEqualTo(6);
    }
}
