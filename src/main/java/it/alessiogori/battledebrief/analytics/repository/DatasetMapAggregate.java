package it.alessiogori.battledebrief.analytics.repository;

public interface DatasetMapAggregate {

    String getMapName();

    Long getSampleMatches();

    Long getSamplePlayers();

    Long getSamplePerformances();

    Long getWonPerformances();

    Long getDestroyedValue();

    Long getLostValue();

    Long getDamageDealt();

    Long getDamageReceived();

    Long getDeploymentValue();
}
