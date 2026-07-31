package it.alessiogori.battledebrief.match.repository;

import it.alessiogori.battledebrief.match.entity.GameMatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameMatchRepository extends JpaRepository<GameMatch, Long> {

    Optional<GameMatch> findByExternalMatchId(String externalMatchId);

    boolean existsByExternalMatchId(String externalMatchId);

    Page<GameMatch> findAllByPerformancesPlayerProfileId(
            Long playerProfileId,
            Pageable pageable
    );
}
