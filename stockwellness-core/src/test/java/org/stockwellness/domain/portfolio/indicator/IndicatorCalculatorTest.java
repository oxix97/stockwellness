package org.stockwellness.domain.portfolio.indicator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.portfolio.math.FinancialMath;
import org.stockwellness.domain.portfolio.vo.ReturnSeries;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Indicator Calculators 단위 테스트")
class IndicatorCalculatorTest {

    private IndicatorCalculator.IndicatorContext context;

    @BeforeEach
    void setUp() {
        Map<LocalDate, BigDecimal> portfolioReturns = new TreeMap<>();
        portfolioReturns.put(LocalDate.of(2026, 1, 1), BigDecimal.valueOf(1.0));
        portfolioReturns.put(LocalDate.of(2026, 1, 2), BigDecimal.valueOf(2.0));
        
        List<BigDecimal> dailyValues = List.of(
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(101),
            BigDecimal.valueOf(103.02) // (100 * 1.01 * 1.02)
        );

        Map<String, ReturnSeries> benchmarkReturns = new HashMap<>();
        Map<LocalDate, BigDecimal> kospiReturns = new TreeMap<>();
        kospiReturns.put(LocalDate.of(2026, 1, 1), BigDecimal.valueOf(0.5));
        kospiReturns.put(LocalDate.of(2026, 1, 2), BigDecimal.valueOf(0.5));
        benchmarkReturns.put("KOSPI", new ReturnSeries(kospiReturns));

        BigDecimal initialAmount = BigDecimal.valueOf(100);
        BigDecimal finalAmount = BigDecimal.valueOf(103.02);
        double years = 2.0 / 252.0;
        BigDecimal portfolioCagr = FinancialMath.calculateCAGR(initialAmount, finalAmount, years);

        context = new IndicatorCalculator.IndicatorContext(
            new ReturnSeries(portfolioReturns),
            dailyValues,
            initialAmount,
            finalAmount,
            years,
            benchmarkReturns,
            "KOSPI",
            BigDecimal.valueOf(3.0),
            portfolioCagr
        );
    }

    @Test
    @DisplayName("PerformanceCalculator 검증")
    void performanceCalculator() {
        PerformanceCalculator calculator = new PerformanceCalculator();
        PerformanceCalculator.PerformanceMetrics metrics = calculator.calculate(context);

        assertThat(metrics.totalReturnRate().doubleValue()).isEqualTo(3.02);
        assertThat(metrics.cagr()).isNotNull();
    }

    @Test
    @DisplayName("RiskCalculator 검증")
    void riskCalculator() {
        RiskCalculator calculator = new RiskCalculator();
        RiskCalculator.RiskMetrics metrics = calculator.calculate(context);

        assertThat(metrics.mdd().doubleValue()).isEqualTo(0.0);
        assertThat(metrics.annualizedVolatility()).isNotNull();
    }

    @Test
    @DisplayName("EfficiencyCalculator 검증")
    void efficiencyCalculator() {
        EfficiencyCalculator calculator = new EfficiencyCalculator();
        EfficiencyCalculator.EfficiencyMetrics metrics = calculator.calculate(context);

        assertThat(metrics.sharpeRatio()).isNotNull();
    }

    @Test
    @DisplayName("MarketCorrelationCalculator 검증")
    void correlationCalculator() {
        MarketCorrelationCalculator calculator = new MarketCorrelationCalculator();
        Map<String, MarketCorrelationCalculator.CorrelationMetrics> metrics = calculator.calculate(context);

        assertThat(metrics).containsKey("KOSPI");
        assertThat(metrics.get("KOSPI").indexReturn().doubleValue()).isEqualTo(1.0025);
    }
}
