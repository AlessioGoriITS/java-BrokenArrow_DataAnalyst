package it.alessiogori.battledebrief.match.repository;

import it.alessiogori.battledebrief.match.entity.GameMatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface GameMatchRepository extends
        JpaRepository<GameMatch, Long>,
        JpaSpecificationExecutor<GameMatch> {

    Optional<GameMatch> findByExternalMatchId(String externalMatchId);

    boolean existsByExternalMatchId(String externalMatchId);

    Page<GameMatch> findAllByPerformancesPlayerProfileId(
            Long playerProfileId,
            Pageable pageable
    );
}
