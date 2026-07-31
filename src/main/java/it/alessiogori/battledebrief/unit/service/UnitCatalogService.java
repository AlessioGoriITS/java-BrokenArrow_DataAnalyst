package it.alessiogori.battledebrief.unit.service;

import it.alessiogori.battledebrief.common.dto.PageResponse;
import it.alessiogori.battledebrief.unit.dto.CreateUnitRequest;
import it.alessiogori.battledebrief.unit.dto.SpecializationRequest;
import it.alessiogori.battledebrief.unit.dto.SpecializationResponse;
import it.alessiogori.battledebrief.unit.dto.UnitResponse;
import it.alessiogori.battledebrief.unit.dto.UnitSearchCriteria;
import it.alessiogori.battledebrief.unit.dto.UpdateUnitRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UnitCatalogService {

    PageResponse<UnitResponse> search(
            UnitSearchCriteria criteria,
            Pageable pageable
    );

    UnitResponse getById(Long unitId);

    UnitResponse create(CreateUnitRequest request);

    UnitResponse update(Long unitId, UpdateUnitRequest request);

    void delete(Long unitId);

    List<SpecializationResponse> findAllSpecializations();

    SpecializationResponse createSpecialization(SpecializationRequest request);
}
