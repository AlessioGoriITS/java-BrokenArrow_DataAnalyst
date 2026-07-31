package it.alessiogori.battledebrief.unit.controller;

import it.alessiogori.battledebrief.unit.dto.SpecializationResponse;
import it.alessiogori.battledebrief.unit.service.UnitCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/specializations")
public class SpecializationController {

    private final UnitCatalogService unitCatalogService;

    public SpecializationController(UnitCatalogService unitCatalogService) {
        this.unitCatalogService = unitCatalogService;
    }

    @GetMapping
    public ResponseEntity<List<SpecializationResponse>> findAll() {
        return ResponseEntity.ok(unitCatalogService.findAllSpecializations());
    }
}
