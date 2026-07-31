package it.alessiogori.battledebrief.analytics.repository;

public interface PlayerUnitAggregate {

    Long getUnitId();

    String getExternalUnitId();

    String getUnitName();

    String getFaction();

    String getCategory();

    Long getSampleMatches();

    Long getSpawnedCount();

    Long getLostCount();

    Long getKillsCount();

    Long getDestroyedValue();

    Long getDeploymentCost();

    Long getLostValue();

    Long getDamageDealt();

    Long getDamageReceived();

    Long getSupplyConsumed();
}
