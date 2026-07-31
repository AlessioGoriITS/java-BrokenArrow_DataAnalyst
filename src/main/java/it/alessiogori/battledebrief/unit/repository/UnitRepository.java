package it.alessiogori.battledebrief.unit.repository;

import it.alessiogori.battledebrief.unit.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    Optional<Unit> findByExternalUnitId(String externalUnitId);

    boolean existsByExternalUnitId(String externalUnitId);
}
