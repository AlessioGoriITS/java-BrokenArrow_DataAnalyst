package it.alessiogori.battledebrief.analytics.repository;

public interface PlayerCareerAggregate {

    Long getMatchCount();

    Long getWinCount();

    Long getDestroyedValue();

    Long getLostValue();

    Long getDamageDealt();

    Long getDamageReceived();

    Long getDeploymentValue();

    Integer getPeakElo();
}
