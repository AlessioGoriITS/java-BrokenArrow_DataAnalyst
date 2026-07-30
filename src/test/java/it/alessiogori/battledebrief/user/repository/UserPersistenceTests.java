package it.alessiogori.battledebrief.user.repository;

import it.alessiogori.battledebrief.player.entity.PlayerProfile;
import it.alessiogori.battledebrief.player.repository.PlayerProfileRepository;
import it.alessiogori.battledebrief.user.entity.AuthProvider;
import it.alessiogori.battledebrief.user.entity.Role;
import it.alessiogori.battledebrief.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class UserPersistenceTests {

    private static final String PASSWORD_HASH =
            "$2a$10$Q7JgN0h8r1h9tBvH5vTt9uM8P5m3D1q0l8o7s6a5b4c3d2e1f0g9h";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsUserWithSafeDefaults() {
        User user = new User("demo", "demo@example.com", PASSWORD_HASH);

        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.isEnabled()).isTrue();
        assertThat(userRepository.findByUsernameIgnoreCase("DEMO"))
                .contains(saved);
        assertThat(userRepository.findByEmailIgnoreCase("DEMO@EXAMPLE.COM"))
                .contains(saved);
    }

    @Test
    void persistsAndReadsOneToOnePlayerProfile() {
        User user = new User("player", "player@example.com", PASSWORD_HASH);
        PlayerProfile profile = new PlayerProfile("Commander Demo");
        profile.updateExternalIdentity(
                "Commander Demo",
                "76561198000000000",
                "commander-demo",
                "https://example.com/avatar.png"
        );
        profile.updateElo(1450, 1520);
        profile.markSynchronized(Instant.parse("2026-07-30T10:15:00Z"));
        user.linkPlayerProfile(profile);

        Long userId = userRepository.saveAndFlush(user).getId();
        entityManager.clear();

        PlayerProfile savedProfile = playerProfileRepository
                .findByUserId(userId)
                .orElseThrow();

        assertThat(savedProfile.getDisplayName()).isEqualTo("Commander Demo");
        assertThat(savedProfile.getSteamId()).isEqualTo("76561198000000000");
        assertThat(savedProfile.getExternalCommanderId())
                .isEqualTo("commander-demo");
        assertThat(savedProfile.getCurrentElo()).isEqualTo(1450);
        assertThat(savedProfile.getPeakElo()).isEqualTo(1520);
        assertThat(savedProfile.getUser().getId()).isEqualTo(userId);
        assertThat(playerProfileRepository.findBySteamId(
                "76561198000000000"
        )).contains(savedProfile);
    }

    @Test
    void rejectsDuplicateUsername() {
        userRepository.saveAndFlush(
                new User("duplicate", "first@example.com", PASSWORD_HASH)
        );

        User duplicate = new User(
                "duplicate",
                "second@example.com",
                PASSWORD_HASH
        );

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void preventsProfileFromBeingLinkedToTwoUsers() {
        User firstUser = new User("first", "first@example.com", PASSWORD_HASH);
        User secondUser = new User("second", "second@example.com", PASSWORD_HASH);
        PlayerProfile profile = new PlayerProfile("Commander");
        firstUser.linkPlayerProfile(profile);

        assertThatThrownBy(() -> secondUser.linkPlayerProfile(profile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Player profile is already linked to another user");
    }
}
