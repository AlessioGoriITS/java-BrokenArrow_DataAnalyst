package it.alessiogori.battledebrief.unit.dataset;

import it.alessiogori.battledebrief.common.exception.ImportValidationException;
import it.alessiogori.battledebrief.unit.repository.SpecializationRepository;
import it.alessiogori.battledebrief.unit.repository.UnitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogDatasetImportTests {

    @Autowired
    private CatalogDatasetReader datasetReader;

    @Autowired
    private CatalogDatasetImportService importService;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private SpecializationRepository specializationRepository;

    @Test
    void classpathDatasetIsReadableAndValid() {
        CatalogDataset dataset = datasetReader.read();

        assertThat(dataset.version()).isEqualTo("barmory-8.4-2026.07");
        assertThat(dataset.specializations()).hasSize(11);
        assertThat(dataset.units())
                .hasSize(420)
                .extracting(CatalogUnitData::faction)
                .containsOnly("USA", "RUS");
    }

    @Test
    void importingTwiceUpdatesCatalogWithoutCreatingDuplicates() {
        CatalogDataset dataset = datasetReader.read();

        CatalogImportResult first = importService.importDataset(dataset);
        CatalogImportResult second = importService.importDataset(dataset);

        assertThat(first).isEqualTo(new CatalogImportResult(11, 420));
        assertThat(second).isEqualTo(first);
        assertThat(specializationRepository.count()).isEqualTo(11);
        assertThat(unitRepository.count()).isEqualTo(420);

        var abrams = unitRepository.findByExternalUnitId("usa_m1a1_abrams")
                .orElseThrow();
        assertThat(abrams.getName()).isEqualTo("M1A1 Abrams");
        assertThat(abrams.getFaction()).isEqualTo("USA");
        assertThat(abrams.getDatasetVersion())
                .isEqualTo("barmory-8.4-2026.07");
        assertThat(abrams.getSpecializations())
                .extracting(specialization -> specialization.getName())
                .containsExactly("Armored Brigade");

        var reostat = unitRepository.findByExternalUnitId("ba_384")
                .orElseThrow();
        assertThat(reostat.getName()).isEqualTo("1V119 Reostat");
        assertThat(reostat.getBaseCost()).isEqualTo(45);
    }

    @Test
    void unknownSpecializationRejectsEntireDataset() {
        CatalogDataset invalid = new CatalogDataset(
                "invalid-1",
                List.of(new CatalogSpecializationData(
                        "Known",
                        "USA",
                        null
                )),
                List.of(new CatalogUnitData(
                        "invalid-unit",
                        "Invalid Unit",
                        "USA",
                        "TANK",
                        100,
                        null,
                        100,
                        BigDecimal.TEN,
                        null,
                        null,
                        null,
                        List.of("Missing")
                ))
        );

        assertThatThrownBy(() -> importService.importDataset(invalid))
                .isInstanceOf(ImportValidationException.class)
                .hasMessageContaining("Unknown specialization 'Missing'");
    }
}
