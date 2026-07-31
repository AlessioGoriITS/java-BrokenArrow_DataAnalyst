package it.alessiogori.battledebrief.analytics.service;

import it.alessiogori.battledebrief.analytics.dto.AnalyticsStatus;
import it.alessiogori.battledebrief.analytics.dto.RatioMetric;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class AnalyticsCalculator {

    private static final int RATIO_SCALE = 4;
    private static final int DISPLAY_SCALE = 2;

    public RatioMetric ratio(
            long numerator,
            long denominator,
            AnalyticsStatus unavailableStatus
    ) {
        if (denominator == 0) {
            return new RatioMetric(null, unavailableStatus);
        }
        return new RatioMetric(
                BigDecimal.valueOf(numerator).divide(
                        BigDecimal.valueOf(denominator),
                        RATIO_SCALE,
                        RoundingMode.HALF_UP
                ),
                AnalyticsStatus.AVAILABLE
        );
    }

    public RatioMetric percentage(
            long numerator,
            long denominator,
            AnalyticsStatus unavailableStatus
    ) {
        if (denominator == 0) {
            return new RatioMetric(null, unavailableStatus);
        }
        return new RatioMetric(
                BigDecimal.valueOf(numerator)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                                BigDecimal.valueOf(denominator),
                                DISPLAY_SCALE,
                                RoundingMode.HALF_UP
                        ),
                AnalyticsStatus.AVAILABLE
        );
    }

    public BigDecimal average(long total, long count) {
        if (count == 0) {
            return null;
        }
        return BigDecimal.valueOf(total).divide(
                BigDecimal.valueOf(count),
                DISPLAY_SCALE,
                RoundingMode.HALF_UP
        );
    }
}
