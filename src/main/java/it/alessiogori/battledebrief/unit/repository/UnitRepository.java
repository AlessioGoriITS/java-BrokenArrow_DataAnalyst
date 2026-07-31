package it.alessiogori.battledebrief.unit.repository;

import it.alessiogori.battledebrief.unit.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UnitRepository extends
        JpaRepository<Unit, Long>,
        JpaSpecificationExecutor<Unit> {

    Optional<Unit> findByExternalUnitId(String externalUnitId);

    boolean existsByExternalUnitId(String externalUnitId);
}
