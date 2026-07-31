package it.alessiogori.battledebrief.analytics.service;

import it.alessiogori.battledebrief.analytics.dto.DatasetUnitAnalyticsResponse;

import java.util.List;

public interface DatasetAnalyticsService {

    List<DatasetUnitAnalyticsResponse> analyzeUnits();
}
