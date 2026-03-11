package org.stockwellness.application.service.portfolio.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PortfolioStatCalculator 단위 테스트")
class PortfolioStatCalculatorTest {

    private final PortfolioStatCalculator calculator = new PortfolioStatCalculator();

    @Test
    @DisplayName("MDD(최대 낙폭) 계산: 시계열 가치 데이터에서 가장 큰 하락폭을 계산한다")
    void calculate_mdd() {
        // [100, 120, 90, 110, 80, 100]
        // Peak 120 -> Trough 80: Drop 40. 40/120 * 100 = 33.3333...
        List<BigDecimal> values = List.of(
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(120),
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(110),
                BigDecimal.valueOf(80),
                BigDecimal.valueOf(100)
        );

        BigDecimal mdd = calculator.calculateMDD(values);

        assertThat(mdd).isEqualByComparingTo(BigDecimal.valueOf(33.3333));
    }

    @Test
    @DisplayName("Sharpe Ratio 계산: 수익률의 평균과 표준편차를 기반으로 위험 대비 수익을 계산한다")
    void calculate_sharpe() {
        // Daily Returns: [1, 2, -1, 2] -> Mean: 1.0, StdDev: 1.2247...
        // Sharpe: 1.0 / 1.2247 = 0.8165...
        List<BigDecimal> returns = List.of(
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(2),
                BigDecimal.valueOf(-1),
                BigDecimal.valueOf(2)
        );

        BigDecimal sharpe = calculator.calculateSharpeRatio(returns);

        assertThat(sharpe).isEqualByComparingTo(BigDecimal.valueOf(0.8165));
    }

    @Test
    @DisplayName("Beta 계산: 시장 지수 대비 포트폴리오의 민감도를 계산한다")
    void calculate_beta() {
        // Portfolio Returns: [1, 2, 3]
        // Market Returns: [1.1, 1.9, 3.2]
        // Covariance(P, M) / Variance(M)
        List<BigDecimal> pReturns = List.of(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3));
        List<BigDecimal> mReturns = List.of(BigDecimal.valueOf(1.1), BigDecimal.valueOf(1.9), BigDecimal.valueOf(3.2));

        BigDecimal beta = calculator.calculateBeta(pReturns, mReturns);

        // Var(M): Mean=2.066, Devs=[-0.966, -0.166, 1.133], Var=0.7488
        // Cov(P,M): P_devs=[-1, 0, 1], Cov=0.7
        // Beta = 0.7 / 0.7488 = 0.9347
        assertThat(beta).isEqualByComparingTo(BigDecimal.valueOf(0.9347));
    }
}
