package it.alessiogori.battledebrief.unit.repository;

import it.alessiogori.battledebrief.unit.entity.Specialization;
import it.alessiogori.battledebrief.unit.entity.Unit;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
class UnitCatalogPersistenceTests {

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private SpecializationRepository specializationRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsUnitWithMultipleSpecializations() {
        Specialization armored = specializationRepository.save(
                new Specialization("Armored", "USA", "Heavy armored forces")
        );
        Specialization airborne = specializationRepository.save(
                new Specialization("Airborne", "USA", "Rapid deployment forces")
        );
        Unit unit = new Unit(
                "unit-m1a2",
                "M1A2 Abrams",
                "USA",
                "TANK",
                300,
                "2026.07"
        );
        unit.addSpecialization(armored);
        unit.addSpecialization(airborne);
        unit.addSpecialization(armored);

        unitRepository.saveAndFlush(unit);
        entityManager.clear();

        Unit savedUnit = unitRepository
                .findByExternalUnitId("unit-m1a2")
                .orElseThrow();

        assertThat(savedUnit.getId()).isNotNull();
        assertThat(savedUnit.getName()).isEqualTo("M1A2 Abrams");
        assertThat(savedUnit.getBaseCost()).isEqualTo(300);
        assertThat(savedUnit.getSpecializations())
                .extracting(Specialization::getName)
                .containsExactlyInAnyOrder("Armored", "Airborne");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from unit_specializations where unit_id = ?",
                Integer.class,
                savedUnit.getId()
        )).isEqualTo(2);
    }

    @Test
    void readsRelationshipFromSpecializationSide() {
        Specialization specialization = specializationRepository.save(
                new Specialization("Mechanized", "RUSSIA", null)
        );
        Unit firstUnit = new Unit(
                "unit-t90",
                "T-90M",
                "RUSSIA",
                "TANK",
                290,
                "2026.07"
        );
        Unit secondUnit = new Unit(
                "unit-bmp3",
                "BMP-3",
                "RUSSIA",
                "VEHICLE",
                180,
                "2026.07"
        );
        firstUnit.addSpecialization(specialization);
        secondUnit.addSpecialization(specialization);
        unitRepository.saveAllAndFlush(java.util.List.of(firstUnit, secondUnit));
        entityManager.clear();

        Specialization savedSpecialization = specializationRepository
                .findByNameIgnoreCaseAndFactionIgnoreCase(
                        "MECHANIZED",
                        "russia"
                )
                .orElseThrow();

        assertThat(savedSpecialization.getUnits())
                .extracting(Unit::getExternalUnitId)
                .containsExactlyInAnyOrder("unit-t90", "unit-bmp3");
    }

    @Test
    void rejectsDuplicateExternalUnitId() {
        unitRepository.saveAndFlush(new Unit(
                "duplicate-id",
                "First Unit",
                "USA",
                "TANK",
                100,
                "2026.07"
        ));

        Unit duplicate = new Unit(
                "duplicate-id",
                "Second Unit",
                "USA",
                "VEHICLE",
                120,
                "2026.07"
        );

        assertThatThrownBy(() -> unitRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateSpecializationWithinFaction() {
        specializationRepository.saveAndFlush(
                new Specialization("Armored", "USA", null)
        );

        Specialization duplicate = new Specialization(
                "Armored",
                "USA",
                "Duplicate"
        );

        assertThatThrownBy(() -> specializationRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
