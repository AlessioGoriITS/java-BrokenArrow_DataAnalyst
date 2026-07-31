package it.alessiogori.battledebrief.match.repository;

import it.alessiogori.battledebrief.analytics.repository.PlayerCareerAggregate;
import it.alessiogori.battledebrief.match.entity.MatchPerformance;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    List<MatchPerformance> findAllByPlayerProfileId(
            Long playerProfileId,
            Pageable pageable
    );

    boolean existsByGameMatchIdAndPlayerProfileUserId(
            Long gameMatchId,
            Long userId
    );

    @Query("""
            select count(performance.id) as matchCount,
                   coalesce(sum(case when performance.won = true
                       then 1L else 0L end), 0L) as winCount,
                   coalesce(sum(performance.destructionScore), 0L)
                       as destroyedValue,
                   coalesce(sum(performance.lossesScore), 0L) as lostValue,
                   coalesce(sum(performance.damageDealt), 0L) as damageDealt,
                   coalesce(sum(performance.damageReceived), 0L)
                       as damageReceived,
                   coalesce(sum(performance.spawnedUnitScore), 0L)
                       as deploymentValue,
                   max(performance.newRating) as peakElo
            from MatchPerformance performance
            where performance.playerProfile.id = :playerProfileId
            """)
    PlayerCareerAggregate aggregateCareer(
            @Param("playerProfileId") Long playerProfileId
    );

    Optional<MatchPerformance>
    findFirstByPlayerProfileIdAndNewRatingIsNotNullOrderByGameMatchStartedAtDesc(
            Long playerProfileId
    );

    Optional<MatchPerformance>
    findFirstByPlayerProfileIdAndOldRatingIsNotNullOrderByGameMatchStartedAtAsc(
            Long playerProfileId
    );
}
