package it.alessiogori.battledebrief.unit.repository;

import it.alessiogori.battledebrief.unit.entity.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpecializationRepository
        extends JpaRepository<Specialization, Long> {

    Optional<Specialization> findByNameIgnoreCaseAndFactionIgnoreCase(
            String name,
            String faction
    );

    boolean existsByNameIgnoreCaseAndFactionIgnoreCase(
            String name,
            String faction
    );
}
