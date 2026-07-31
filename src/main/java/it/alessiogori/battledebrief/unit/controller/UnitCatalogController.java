package it.alessiogori.battledebrief.unit.controller;

import it.alessiogori.battledebrief.common.dto.PageResponse;
import it.alessiogori.battledebrief.unit.dto.UnitResponse;
import it.alessiogori.battledebrief.unit.dto.UnitSearchCriteria;
import it.alessiogori.battledebrief.unit.service.UnitCatalogService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/units")
public class UnitCatalogController {

    private final UnitCatalogService unitCatalogService;

    public UnitCatalogController(UnitCatalogService unitCatalogService) {
        this.unitCatalogService = unitCatalogService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<UnitResponse>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String faction,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long specializationId,
            @RequestParam(required = false) Integer minCost,
            @RequestParam(required = false) Integer maxCost,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        UnitSearchCriteria criteria = new UnitSearchCriteria(
                name,
                faction,
                category,
                specializationId,
                minCost,
                maxCost
        );
        return ResponseEntity.ok(unitCatalogService.search(criteria, pageable));
    }

    @GetMapping("/{unitId}")
    public ResponseEntity<UnitResponse> getById(@PathVariable Long unitId) {
        return ResponseEntity.ok(unitCatalogService.getById(unitId));
    }
}
