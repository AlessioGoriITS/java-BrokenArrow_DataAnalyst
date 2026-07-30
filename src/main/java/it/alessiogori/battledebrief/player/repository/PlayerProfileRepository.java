package it.alessiogori.battledebrief.player.repository;

import it.alessiogori.battledebrief.player.entity.PlayerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerProfileRepository
        extends JpaRepository<PlayerProfile, Long> {

    Optional<PlayerProfile> findByUserId(Long userId);

    Optional<PlayerProfile> findBySteamId(String steamId);

    Optional<PlayerProfile> findByExternalCommanderId(String externalCommanderId);
}
