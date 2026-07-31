package it.alessiogori.battledebrief.unit.controller;

import it.alessiogori.battledebrief.unit.dto.CreateUnitRequest;
import it.alessiogori.battledebrief.unit.dto.UnitResponse;
import it.alessiogori.battledebrief.unit.dto.UpdateUnitRequest;
import it.alessiogori.battledebrief.unit.service.UnitCatalogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/admin/units")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUnitController {

    private final UnitCatalogService unitCatalogService;

    public AdminUnitController(UnitCatalogService unitCatalogService) {
        this.unitCatalogService = unitCatalogService;
    }

    @PostMapping
    public ResponseEntity<UnitResponse> create(
            @Valid @RequestBody CreateUnitRequest request
    ) {
        UnitResponse created = unitCatalogService.create(request);
        return ResponseEntity
                .created(URI.create("/api/units/" + created.id()))
                .body(created);
    }

    @PutMapping("/{unitId}")
    public ResponseEntity<UnitResponse> update(
            @PathVariable Long unitId,
            @Valid @RequestBody UpdateUnitRequest request
    ) {
        return ResponseEntity.ok(unitCatalogService.update(unitId, request));
    }

    @DeleteMapping("/{unitId}")
    public ResponseEntity<Void> delete(@PathVariable Long unitId) {
        unitCatalogService.delete(unitId);
        return ResponseEntity.noContent().build();
    }
}
