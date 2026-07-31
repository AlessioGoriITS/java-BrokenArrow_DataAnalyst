package it.alessiogori.battledebrief.match.repository;

import it.alessiogori.battledebrief.analytics.repository.DatasetSpecializationAggregate;
import it.alessiogori.battledebrief.analytics.repository.DatasetUnitAggregate;
import it.alessiogori.battledebrief.analytics.repository.PlayerUnitAggregate;
import it.alessiogori.battledebrief.match.entity.UnitMatchPerformance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UnitMatchPerformanceRepository
        extends JpaRepository<UnitMatchPerformance, Long> {

    List<UnitMatchPerformance> findAllByMatchPerformanceId(
            Long matchPerformanceId
    );

    List<UnitMatchPerformance> findAllByMatchPerformancePlayerProfileIdAndUnitId(
            Long playerProfileId,
            Long unitId
    );

    Page<UnitMatchPerformance>
    findAllByMatchPerformancePlayerProfileIdAndUnitId(
            Long playerProfileId,
            Long unitId,
            Pageable pageable
    );

    boolean existsByMatchPerformancePlayerProfileIdAndUnitId(
            Long playerProfileId,
            Long unitId
    );

    @Query("""
            select performance.unit.id as unitId,
                   performance.unit.externalUnitId as externalUnitId,
                   performance.unit.name as unitName,
                   performance.unit.faction as faction,
                   performance.unit.category as category,
                   count(distinct performance.matchPerformance.gameMatch.id)
                       as sampleMatches,
                   sum(performance.spawnedCount) as spawnedCount,
                   sum(performance.lostCount) as lostCount,
                   sum(performance.killsCount) as killsCount,
                   sum(performance.destroyedValue) as destroyedValue,
                   sum(performance.unitCost * performance.spawnedCount)
                       as deploymentCost,
                   sum(performance.unitCost * performance.lostCount)
                       as lostValue,
                   coalesce(sum(performance.damageDealt), 0L) as damageDealt,
                   coalesce(sum(performance.damageReceived), 0L)
                       as damageReceived,
                   coalesce(sum(performance.supplyConsumed), 0L)
                       as supplyConsumed
            from UnitMatchPerformance performance
            where performance.matchPerformance.playerProfile.id =
                :playerProfileId
            group by performance.unit.id,
                     performance.unit.externalUnitId,
                     performance.unit.name,
                     performance.unit.faction,
                     performance.unit.category
            order by sum(performance.spawnedCount) desc,
                     performance.unit.name asc
            """)
    List<PlayerUnitAggregate> aggregateByPlayer(
            @Param("playerProfileId") Long playerProfileId
    );

    @Query("""
            select performance.unit.id as unitId,
                   performance.unit.externalUnitId as externalUnitId,
                   performance.unit.name as unitName,
                   performance.unit.faction as faction,
                   performance.unit.category as category,
                   count(distinct performance.matchPerformance.gameMatch.id)
                       as sampleMatches,
                   sum(performance.spawnedCount) as spawnedCount,
                   sum(performance.lostCount) as lostCount,
                   sum(performance.killsCount) as killsCount,
                   sum(performance.destroyedValue) as destroyedValue,
                   sum(performance.unitCost * performance.spawnedCount)
                       as deploymentCost,
                   sum(performance.unitCost * performance.lostCount)
                       as lostValue,
                   coalesce(sum(performance.damageDealt), 0L) as damageDealt,
                   coalesce(sum(performance.damageReceived), 0L)
                       as damageReceived,
                   coalesce(sum(performance.supplyConsumed), 0L)
                       as supplyConsumed
            from UnitMatchPerformance performance
            where performance.matchPerformance.playerProfile.id =
                :playerProfileId
              and performance.unit.id = :unitId
            group by performance.unit.id,
                     performance.unit.externalUnitId,
                     performance.unit.name,
                     performance.unit.faction,
                     performance.unit.category
            """)
    Optional<PlayerUnitAggregate> aggregateByPlayerAndUnit(
            @Param("playerProfileId") Long playerProfileId,
            @Param("unitId") Long unitId
    );

    @Query("""
            select performance.unit.id as unitId,
                   performance.unit.externalUnitId as externalUnitId,
                   performance.unit.name as unitName,
                   performance.unit.faction as faction,
                   performance.unit.category as category,
                   count(distinct performance.matchPerformance.gameMatch.id)
                       as sampleMatches,
                   count(distinct performance.matchPerformance.playerProfile.id)
                       as samplePlayers,
                   count(performance.id) as samplePerformances,
                   sum(case when performance.matchPerformance.won = true
                       then 1L else 0L end) as wonPerformances,
                   sum(performance.spawnedCount) as spawnedCount,
                   sum(performance.lostCount) as lostCount,
                   sum(performance.destroyedValue) as destroyedValue,
                   sum(performance.unitCost * performance.spawnedCount)
                       as deploymentCost,
                   sum(performance.unitCost * performance.lostCount)
                       as lostValue
            from UnitMatchPerformance performance
            group by performance.unit.id,
                     performance.unit.externalUnitId,
                     performance.unit.name,
                     performance.unit.faction,
                     performance.unit.category
            order by sum(performance.spawnedCount) desc,
                     performance.unit.name asc
            """)
    List<DatasetUnitAggregate> aggregateDatasetByUnit();

    @Query("""
            select performance.unit.id as unitId,
                   performance.unit.externalUnitId as externalUnitId,
                   performance.unit.name as unitName,
                   performance.unit.faction as faction,
                   performance.unit.category as category,
                   count(distinct performance.matchPerformance.gameMatch.id)
                       as sampleMatches,
                   count(distinct performance.matchPerformance.playerProfile.id)
                       as samplePlayers,
                   count(performance.id) as samplePerformances,
                   sum(case when performance.matchPerformance.won = true
                       then 1L else 0L end) as wonPerformances,
                   sum(performance.spawnedCount) as spawnedCount,
                   sum(performance.lostCount) as lostCount,
                   sum(performance.destroyedValue) as destroyedValue,
                   sum(performance.unitCost * performance.spawnedCount)
                       as deploymentCost,
                   sum(performance.unitCost * performance.lostCount)
                       as lostValue
            from UnitMatchPerformance performance
            where performance.unit.id = :unitId
            group by performance.unit.id,
                     performance.unit.externalUnitId,
                     performance.unit.name,
                     performance.unit.faction,
                     performance.unit.category
            """)
    Optional<DatasetUnitAggregate> aggregateDatasetByUnitId(
            @Param("unitId") Long unitId
    );

    @Query("""
            select specialization.id as specializationId,
                   specialization.name as specializationName,
                   specialization.faction as faction,
                   count(distinct performance.matchPerformance.gameMatch.id)
                       as sampleMatches,
                   count(distinct performance.matchPerformance.playerProfile.id)
                       as samplePlayers,
                   count(distinct performance.unit.id) as sampleUnits,
                   count(distinct performance.matchPerformance.id)
                       as samplePerformances,
                   count(distinct case
                       when performance.matchPerformance.won = true
                       then performance.matchPerformance.id else null end)
                       as wonPerformances,
                   sum(performance.spawnedCount) as spawnedCount,
                   sum(performance.lostCount) as lostCount,
                   sum(performance.destroyedValue) as destroyedValue,
                   sum(performance.unitCost * performance.spawnedCount)
                       as deploymentCost,
                   sum(performance.unitCost * performance.lostCount)
                       as lostValue
            from UnitMatchPerformance performance
            join performance.unit.specializations specialization
            group by specialization.id,
                     specialization.name,
                     specialization.faction
            order by count(distinct performance.matchPerformance.gameMatch.id)
                         desc,
                     specialization.faction asc,
                     specialization.name asc
            """)
    List<DatasetSpecializationAggregate>
    aggregateDatasetBySpecialization();
}
