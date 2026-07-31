package it.alessiogori.battledebrief.analytics.service;

import it.alessiogori.battledebrief.analytics.dto.AnalyticsStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsCalculatorTests {

    private final AnalyticsCalculator calculator = new AnalyticsCalculator();

    @Test
    void calculatesRatiosPercentagesAndAveragesWithStableScale() {
        assertThat(calculator.ratio(
                1,
                3,
                AnalyticsStatus.NO_LOSSES
        ).value()).isEqualByComparingTo(new BigDecimal("0.3333"));
        assertThat(calculator.percentage(
                2,
                3,
                AnalyticsStatus.NO_MATCHES
        ).value()).isEqualByComparingTo(new BigDecimal("66.67"));
        assertThat(calculator.average(10, 4))
                .isEqualByComparingTo(new BigDecimal("2.50"));
    }

    @Test
    void zeroDenominatorReturnsNullAndExplicitStatus() {
        var result = calculator.ratio(
                100,
                0,
                AnalyticsStatus.NO_LOSSES
        );

        assertThat(result.value()).isNull();
        assertThat(result.status()).isEqualTo(AnalyticsStatus.NO_LOSSES);
        assertThat(calculator.average(100, 0)).isNull();
    }
}
