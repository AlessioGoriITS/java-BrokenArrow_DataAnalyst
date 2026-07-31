package it.alessiogori.battledebrief.analytics.repository;

public interface DatasetSpecializationAggregate {

    Long getSpecializationId();

    String getSpecializationName();

    String getFaction();

    Long getSampleMatches();

    Long getSamplePlayers();

    Long getSampleUnits();

    Long getSamplePerformances();

    Long getWonPerformances();

    Long getSpawnedCount();

    Long getLostCount();

    Long getDestroyedValue();

    Long getDeploymentCost();

    Long getLostValue();
}
