package it.alessiogori.battledebrief.analytics.controller;

import it.alessiogori.battledebrief.analytics.dto.DatasetUnitAnalyticsResponse;
import it.alessiogori.battledebrief.analytics.dto.DatasetMapAnalyticsResponse;
import it.alessiogori.battledebrief.analytics.dto.DatasetSpecializationAnalyticsResponse;
import it.alessiogori.battledebrief.analytics.service.DatasetAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class DatasetAnalyticsController {

    private final DatasetAnalyticsService analyticsService;

    public DatasetAnalyticsController(
            DatasetAnalyticsService analyticsService
    ) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/units")
    public ResponseEntity<List<DatasetUnitAnalyticsResponse>> findUnits() {
        return ResponseEntity.ok(analyticsService.analyzeUnits());
    }

    @GetMapping("/units/{unitId}")
    public ResponseEntity<DatasetUnitAnalyticsResponse> findUnit(
            @PathVariable Long unitId
    ) {
        return ResponseEntity.ok(analyticsService.analyzeUnit(unitId));
    }

    @GetMapping("/maps")
    public ResponseEntity<List<DatasetMapAnalyticsResponse>> findMaps() {
        return ResponseEntity.ok(analyticsService.analyzeMaps());
    }

    @GetMapping("/specializations")
    public ResponseEntity<List<DatasetSpecializationAnalyticsResponse>>
    findSpecializations() {
        return ResponseEntity.ok(analyticsService.analyzeSpecializations());
    }
}
