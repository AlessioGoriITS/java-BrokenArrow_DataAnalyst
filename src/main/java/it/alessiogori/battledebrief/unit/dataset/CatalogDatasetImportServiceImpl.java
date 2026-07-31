package it.alessiogori.battledebrief.unit.dataset;

import it.alessiogori.battledebrief.common.exception.ImportValidationException;
import it.alessiogori.battledebrief.unit.entity.Specialization;
import it.alessiogori.battledebrief.unit.entity.Unit;
import it.alessiogori.battledebrief.unit.repository.SpecializationRepository;
import it.alessiogori.battledebrief.unit.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CatalogDatasetImportServiceImpl
        implements CatalogDatasetImportService {

    private final SpecializationRepository specializationRepository;
    private final UnitRepository unitRepository;

    public CatalogDatasetImportServiceImpl(
            SpecializationRepository specializationRepository,
            UnitRepository unitRepository
    ) {
        this.specializationRepository = specializationRepository;
        this.unitRepository = unitRepository;
    }

    @Override
    @Transactional
    public CatalogImportResult importDataset(CatalogDataset dataset) {
        Map<String, Specialization> specializations =
                importSpecializations(dataset.specializations());
        importUnits(dataset, specializations);
        return new CatalogImportResult(
                dataset.specializations().size(),
                dataset.units().size()
        );
    }

    private Map<String, Specialization> importSpecializations(
            List<CatalogSpecializationData> records
    ) {
        Map<String, Specialization> imported = new HashMap<>();
        for (CatalogSpecializationData record : records) {
            String name = record.name().trim();
            String faction = normalizeCode(record.faction());
            String key = specializationKey(faction, name);
            if (imported.containsKey(key)) {
                throw new ImportValidationException(
                        "Duplicate specialization in catalog dataset: " + name
                );
            }

            Specialization specialization = specializationRepository
                    .findByNameIgnoreCaseAndFactionIgnoreCase(name, faction)
                    .orElseGet(() -> new Specialization(
                            name,
                            faction,
                            record.description()
                    ));
            specialization.updateDetails(name, faction, record.description());
            imported.put(key, specializationRepository.save(specialization));
        }
        specializationRepository.flush();
        return imported;
    }

    private void importUnits(
            CatalogDataset dataset,
            Map<String, Specialization> specializations
    ) {
        Set<String> externalIds = new HashSet<>();
        for (CatalogUnitData record : dataset.units()) {
            String externalId = record.externalUnitId().trim();
            if (!externalIds.add(externalId)) {
                throw new ImportValidationException(
                        "Duplicate externalUnitId in catalog dataset: "
                                + externalId
                );
            }

            String faction = normalizeCode(record.faction());
            Unit unit = unitRepository.findByExternalUnitId(externalId)
                    .orElseGet(() -> new Unit(
                            externalId,
                            record.name().trim(),
                            faction,
                            normalizeCode(record.category()),
                            record.baseCost(),
                            dataset.version().trim()
                    ));
            unit.updateDetails(
                    record.name().trim(),
                    faction,
                    normalizeCode(record.category()),
                    record.baseCost(),
                    record.description(),
                    record.hitPoints(),
                    record.speed(),
                    record.armor(),
                    record.mainWeapon(),
                    record.imageUrl(),
                    dataset.version().trim()
            );
            unit.replaceSpecializations(resolveSpecializations(
                    record,
                    faction,
                    specializations
            ));
            unitRepository.save(unit);
        }
        unitRepository.flush();
    }

    private List<Specialization> resolveSpecializations(
            CatalogUnitData unit,
            String faction,
            Map<String, Specialization> specializations
    ) {
        return unit.specializations().stream()
                .map(name -> {
                    Specialization specialization = specializations.get(
                            specializationKey(faction, name)
                    );
                    if (specialization == null) {
                        throw new ImportValidationException(
                                "Unknown specialization '" + name
                                        + "' for unit "
                                        + unit.externalUnitId()
                        );
                    }
                    return specialization;
                })
                .distinct()
                .toList();
    }

    private String specializationKey(String faction, String name) {
        return normalizeCode(faction) + "\u0000"
                + name.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
