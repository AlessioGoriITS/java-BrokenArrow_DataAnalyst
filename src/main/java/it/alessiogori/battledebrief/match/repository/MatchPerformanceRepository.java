package it.alessiogori.battledebrief.match.repository;

import it.alessiogori.battledebrief.match.entity.MatchPerformance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchPerformanceRepository
        extends JpaRepository<MatchPerformance, Long> {

    Optional<MatchPerformance> findByGameMatchIdAndPlayerProfileId(
            Long gameMatchId,
            Long playerProfileId
    );

    List<MatchPerformance> findAllByPlayerProfileIdOrderByGameMatchStartedAtDesc(
            Long playerProfileId
    );
}
