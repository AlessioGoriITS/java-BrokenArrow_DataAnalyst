package it.alessiogori.battledebrief.unit.controller;

import it.alessiogori.battledebrief.unit.dto.SpecializationRequest;
import it.alessiogori.battledebrief.unit.dto.SpecializationResponse;
import it.alessiogori.battledebrief.unit.service.UnitCatalogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/admin/specializations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSpecializationController {

    private final UnitCatalogService unitCatalogService;

    public AdminSpecializationController(UnitCatalogService unitCatalogService) {
        this.unitCatalogService = unitCatalogService;
    }

    @PostMapping
    public ResponseEntity<SpecializationResponse> create(
            @Valid @RequestBody SpecializationRequest request
    ) {
        SpecializationResponse created =
                unitCatalogService.createSpecialization(request);
        return ResponseEntity
                .created(URI.create("/api/specializations/" + created.id()))
                .body(created);
    }
}
