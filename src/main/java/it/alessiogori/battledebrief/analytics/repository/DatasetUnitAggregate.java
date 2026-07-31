package it.alessiogori.battledebrief.analytics.repository;

public interface DatasetUnitAggregate {

    Long getUnitId();

    String getExternalUnitId();

    String getUnitName();

    String getFaction();

    String getCategory();

    Long getSampleMatches();

    Long getSamplePlayers();

    Long getSamplePerformances();

    Long getWonPerformances();

    Long getSpawnedCount();

    Long getLostCount();

    Long getDestroyedValue();

    Long getDeploymentCost();

    Long getLostValue();
}
