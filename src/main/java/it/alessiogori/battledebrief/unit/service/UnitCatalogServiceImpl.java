package it.alessiogori.battledebrief.unit.service;

import it.alessiogori.battledebrief.common.dto.PageResponse;
import it.alessiogori.battledebrief.common.exception.DuplicateResourceException;
import it.alessiogori.battledebrief.common.exception.InvalidRequestException;
import it.alessiogori.battledebrief.common.exception.ResourceNotFoundException;
import it.alessiogori.battledebrief.unit.dto.CreateUnitRequest;
import it.alessiogori.battledebrief.unit.dto.SpecializationRequest;
import it.alessiogori.battledebrief.unit.dto.SpecializationResponse;
import it.alessiogori.battledebrief.unit.dto.UnitResponse;
import it.alessiogori.battledebrief.unit.dto.UnitSearchCriteria;
import it.alessiogori.battledebrief.unit.dto.UpdateUnitRequest;
import it.alessiogori.battledebrief.unit.entity.Specialization;
import it.alessiogori.battledebrief.unit.entity.Unit;
import it.alessiogori.battledebrief.unit.mapper.UnitMapper;
import it.alessiogori.battledebrief.unit.repository.SpecializationRepository;
import it.alessiogori.battledebrief.unit.repository.UnitRepository;
import it.alessiogori.battledebrief.unit.repository.UnitSpecifications;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class UnitCatalogServiceImpl implements UnitCatalogService {

    private final UnitRepository unitRepository;
    private final SpecializationRepository specializationRepository;
    private final UnitMapper unitMapper;

    public UnitCatalogServiceImpl(
            UnitRepository unitRepository,
            SpecializationRepository specializationRepository,
            UnitMapper unitMapper
    ) {
        this.unitRepository = unitRepository;
        this.specializationRepository = specializationRepository;
        this.unitMapper = unitMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UnitResponse> search(
            UnitSearchCriteria criteria,
            Pageable pageable
    ) {
        validateCostRange(criteria);
        return PageResponse.from(unitRepository
                .findAll(UnitSpecifications.matching(criteria), pageable)
                .map(unitMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public UnitResponse getById(Long unitId) {
        return unitMapper.toResponse(requireUnit(unitId));
    }

    @Override
    @Transactional
    public UnitResponse create(CreateUnitRequest request) {
        String externalUnitId = request.externalUnitId().trim();
        if (unitRepository.existsByExternalUnitId(externalUnitId)) {
            throw new DuplicateResourceException("External unit ID already exists");
        }

        Unit unit = new Unit(
                externalUnitId,
                request.name().trim(),
                normalizeCode(request.faction()),
                normalizeCode(request.category()),
                request.baseCost(),
                request.datasetVersion().trim()
        );
        applyDetails(unit, request);
        unit.replaceSpecializations(resolveSpecializations(
                request.specializationIds()
        ));

        try {
            return unitMapper.toResponse(unitRepository.saveAndFlush(unit));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException(
                    "External unit ID already exists"
            );
        }
    }

    @Override
    @Transactional
    public UnitResponse update(Long unitId, UpdateUnitRequest request) {
        Unit unit = requireUnit(unitId);
        unit.updateDetails(
                request.name().trim(),
                normalizeCode(request.faction()),
                normalizeCode(request.category()),
                request.baseCost(),
                request.description(),
                request.hitPoints(),
                request.speed(),
                request.armor(),
                request.mainWeapon(),
                request.imageUrl(),
                request.datasetVersion().trim()
        );
        unit.replaceSpecializations(resolveSpecializations(
                request.specializationIds()
        ));

        return unitMapper.toResponse(unitRepository.saveAndFlush(unit));
    }

    @Override
    @Transactional
    public void delete(Long unitId) {
        unitRepository.delete(requireUnit(unitId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpecializationResponse> findAllSpecializations() {
        return specializationRepository
                .findAllByOrderByFactionAscNameAsc()
                .stream()
                .map(unitMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SpecializationResponse createSpecialization(
            SpecializationRequest request
    ) {
        String name = request.name().trim();
        String faction = normalizeCode(request.faction());
        if (specializationRepository
                .existsByNameIgnoreCaseAndFactionIgnoreCase(name, faction)) {
            throw new DuplicateResourceException(
                    "Specialization already exists for this faction"
            );
        }

        Specialization specialization = new Specialization(
                name,
                faction,
                request.description()
        );
        try {
            return unitMapper.toResponse(
                    specializationRepository.saveAndFlush(specialization)
            );
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException(
                    "Specialization already exists for this faction"
            );
        }
    }

    private Unit requireUnit(Long unitId) {
        return unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unit not found"
                ));
    }

    private List<Specialization> resolveSpecializations(Set<Long> ids) {
        List<Specialization> specializations = specializationRepository
                .findAllById(ids);
        if (specializations.size() != ids.size()) {
            throw new ResourceNotFoundException(
                    "One or more specializations were not found"
            );
        }
        return specializations;
    }

    private void validateCostRange(UnitSearchCriteria criteria) {
        if (criteria.minCost() != null && criteria.minCost() < 0) {
            throw new InvalidRequestException("minCost cannot be negative");
        }
        if (criteria.maxCost() != null && criteria.maxCost() < 0) {
            throw new InvalidRequestException("maxCost cannot be negative");
        }
        if (criteria.minCost() != null
                && criteria.maxCost() != null
                && criteria.minCost() > criteria.maxCost()) {
            throw new InvalidRequestException(
                    "minCost cannot be greater than maxCost"
            );
        }
    }

    private void applyDetails(Unit unit, CreateUnitRequest request) {
        unit.updateDetails(
                request.name().trim(),
                normalizeCode(request.faction()),
                normalizeCode(request.category()),
                request.baseCost(),
                request.description(),
                request.hitPoints(),
                request.speed(),
                request.armor(),
                request.mainWeapon(),
                request.imageUrl(),
                request.datasetVersion().trim()
        );
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
