package org.stockwellness.application.service.portfolio.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PortfolioCorrelationCalculator 단위 테스트")
class PortfolioCorrelationCalculatorTest {

    private final PortfolioCorrelationCalculator calculator = new PortfolioCorrelationCalculator();

    @Test
    @DisplayName("상관계수 계산: 두 종목의 수익률 리스트를 기반으로 상관계수를 산출한다")
    void calculate_correlation() {
        // given
        List<BigDecimal> returnsA = List.of(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3));
        List<BigDecimal> returnsB = List.of(BigDecimal.valueOf(1.1), BigDecimal.valueOf(1.9), BigDecimal.valueOf(3.2));

        // when
        BigDecimal correlation = calculator.calculateCorrelation(returnsA, returnsB);

        // then
        // Same as beta test logic, but normalized by both variances.
        // Cov(A,B) = 0.7
        // Var(A) = 0.6666...
        // Var(B) = 0.7488...
        // Corr = 0.7 / sqrt(0.6666 * 0.7488) = 0.7 / 0.7066 = 0.9907
        assertThat(correlation).isEqualByComparingTo(BigDecimal.valueOf(0.9907));
    }

    @Test
    @DisplayName("상관계수 행렬 생성: 여러 종목 간의 상관계수를 2차원 맵 형태로 반환한다")
    void calculate_correlation_matrix() {
        // given
        Map<String, List<BigDecimal>> returnsMap = Map.of(
                "AAPL", List.of(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3)),
                "TSLA", List.of(BigDecimal.valueOf(1.1), BigDecimal.valueOf(1.9), BigDecimal.valueOf(3.2))
        );

        // when
        Map<String, Map<String, BigDecimal>> matrix = calculator.calculateMatrix(returnsMap);

        // then
        assertThat(matrix.get("AAPL").get("AAPL")).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(matrix.get("AAPL").get("TSLA")).isEqualByComparingTo(BigDecimal.valueOf(0.9907));
    }
}
