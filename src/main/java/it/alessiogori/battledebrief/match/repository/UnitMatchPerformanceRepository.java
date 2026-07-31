package it.alessiogori.battledebrief.match.repository;

import it.alessiogori.battledebrief.match.entity.UnitMatchPerformance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnitMatchPerformanceRepository
        extends JpaRepository<UnitMatchPerformance, Long> {

    List<UnitMatchPerformance> findAllByMatchPerformanceId(
            Long matchPerformanceId
    );

    List<UnitMatchPerformance> findAllByMatchPerformancePlayerProfileIdAndUnitId(
            Long playerProfileId,
            Long unitId
    );
}
