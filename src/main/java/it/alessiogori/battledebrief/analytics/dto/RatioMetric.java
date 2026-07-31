package it.alessiogori.battledebrief.analytics.dto;

import java.math.BigDecimal;

public record RatioMetric(
        BigDecimal value,
        AnalyticsStatus status
) {
}
