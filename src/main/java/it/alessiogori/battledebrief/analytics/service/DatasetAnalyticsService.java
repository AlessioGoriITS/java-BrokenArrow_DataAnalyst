package it.alessiogori.battledebrief.analytics.service;

import it.alessiogori.battledebrief.analytics.dto.DatasetUnitAnalyticsResponse;
import it.alessiogori.battledebrief.analytics.dto.DatasetMapAnalyticsResponse;
import it.alessiogori.battledebrief.analytics.dto.DatasetSpecializationAnalyticsResponse;

import java.util.List;

public interface DatasetAnalyticsService {

    List<DatasetUnitAnalyticsResponse> analyzeUnits();

    DatasetUnitAnalyticsResponse analyzeUnit(Long unitId);

    List<DatasetMapAnalyticsResponse> analyzeMaps();

    List<DatasetSpecializationAnalyticsResponse> analyzeSpecializations();
}
