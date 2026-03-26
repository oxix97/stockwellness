package org.stockwellness.application.service.portfolio.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BacktestCalculatorTest {

    @Test
    @DisplayName("백테스트 결과로부터 CAGR, MDD, Sharpe Ratio가 정확히 계산되어야 한다")
    void calculatePerformanceIndicators() {
        // given: 1년간(252영업일) 매일 0.1%씩 상승하는 가상의 데이터 생성
        List<BacktestResult.DailyBacktestResult> dailyResults = new ArrayList<>();
        BigDecimal startValue = BigDecimal.valueOf(1000000); // 100만원 시작
        BigDecimal currentValue = startValue;
        LocalDate startDate = LocalDate.of(2023, 1, 1);

        for (int i = 0; i < 252; i++) {
            currentValue = currentValue.multiply(BigDecimal.valueOf(1.001)).setScale(4, RoundingMode.HALF_UP);
            BigDecimal returnRate = currentValue.subtract(startValue)
                    .divide(startValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            dailyResults.add(new BacktestResult.DailyBacktestResult(
                    startDate.plusDays(i),
                    currentValue,
                    startValue,
                    returnRate,
                    Map.of("KOSPI", returnRate.subtract(BigDecimal.ONE)) // 벤치마크는 1%p 낮음
            ));
        }

        // when
        BacktestResult result = BacktestCalculator.calculate(dailyResults);

        // then: CAGR 검증 (약 (1.001^252 - 1) * 100 = 28.6%)
        assertThat(result.cagr()).isGreaterThan(BigDecimal.valueOf(28.0));
        
        // then: MDD 검증 (계속 상승했으므로 0이어야 함)
        assertThat(result.mdd()).isEqualByComparingTo(BigDecimal.ZERO);
        
        // then: Sharpe Ratio 검증 (0이 아니어야 함 - 현재 구현은 0을 반환하므로 여기서 실패 예상)
        assertThat(result.sharpeRatio()).isGreaterThan(BigDecimal.ZERO);
        
        // then: Best/Worst Year 검증
        assertThat(result.bestYearRate()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.worstYearRate()).isNotNull();
    }

    @Test
    @DisplayName("하락장이 포함된 경우 MDD가 정확히 계산되어야 한다")
    void calculateMDDWithDrawdown() {
        // given: 100 -> 120 -> 80 -> 110 (Peak 120, Trough 80 => Drawdown (120-80)/120 = 33.33%)
        List<BacktestResult.DailyBacktestResult> dailyResults = List.of(
            createDailyResult(LocalDate.of(2023, 1, 1), 100),
            createDailyResult(LocalDate.of(2023, 1, 2), 120),
            createDailyResult(LocalDate.of(2023, 1, 3), 80),
            createDailyResult(LocalDate.of(2023, 1, 4), 110)
        );

        // when
        BacktestResult result = BacktestCalculator.calculate(dailyResults);
        System.out.println("Actual MDD: " + result.mdd());

        // then: MDD = 33.33%
        assertThat(result.mdd()).isEqualByComparingTo(BigDecimal.valueOf(33.3333).setScale(4, RoundingMode.HALF_UP));
    }

    private BacktestResult.DailyBacktestResult createDailyResult(LocalDate date, double value) {
        return new BacktestResult.DailyBacktestResult(
                date,
                BigDecimal.valueOf(value),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(value - 100),
                Collections.emptyMap()
        );
    }
}
