package org.stockwellness.application.service.portfolio.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BacktestEngine 단위 테스트")
class BacktestEngineTest {

    private final BacktestEngine backtestEngine = new BacktestEngine();

    @Test
    @DisplayName("거치식(Lump-sum) 백테스팅: 시작일에 전액 매수 후 수익률 변화를 계산한다")
    void lump_sum_backtest() {
        // given
        LocalDate day1 = LocalDate.of(2024, 1, 1);
        LocalDate day2 = LocalDate.of(2024, 1, 2);
        
        StockPriceResult p1 = new StockPriceResult(day1, BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), 100L);
        StockPriceResult p2 = new StockPriceResult(day2, BigDecimal.valueOf(110), BigDecimal.valueOf(110), BigDecimal.valueOf(110), BigDecimal.valueOf(110), BigDecimal.valueOf(110), 100L);
        
        SimulationData data = new SimulationData(
            Map.of("AAPL", List.of(p1, p2)),
            List.of(p1, p2) // Benchmark is same for simplicity
        );
        
        // Portfolio: AAPL 100%
        Map<String, BigDecimal> weights = Map.of("AAPL", BigDecimal.valueOf(100));
        BigDecimal initialAmount = BigDecimal.valueOf(1000);

        // when
        BacktestResult result = backtestEngine.runLumpSum(data, weights, initialAmount);

        // then
        assertThat(result.dailyResults()).hasSize(2);
        // Day 1: 1000 invested in AAPL (10 shares) -> value 1000
        assertThat(result.dailyResults().get(0).totalValue()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        // Day 2: 10 shares * 110 = 1100
        assertThat(result.dailyResults().get(1).totalValue()).isEqualByComparingTo(BigDecimal.valueOf(1100));
        assertThat(result.dailyResults().get(1).returnRate()).isEqualByComparingTo(BigDecimal.valueOf(10)); // 10%
    }

    @Test
    @DisplayName("적립식(DCA) 백테스팅: 매월 정해진 금액을 추가 매수하며 수익률을 계산한다")
    void dca_backtest() {
        // given
        LocalDate month1 = LocalDate.of(2024, 1, 1);
        LocalDate month2 = LocalDate.of(2024, 2, 1);
        
        StockPriceResult p1 = new StockPriceResult(month1, BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), 100L);
        StockPriceResult p2 = new StockPriceResult(month2, BigDecimal.valueOf(200), BigDecimal.valueOf(200), BigDecimal.valueOf(200), BigDecimal.valueOf(200), BigDecimal.valueOf(200), 100L);
        
        SimulationData data = new SimulationData(
            Map.of("AAPL", List.of(p1, p2)),
            List.of(p1, p2)
        );
        
        Map<String, BigDecimal> weights = Map.of("AAPL", BigDecimal.valueOf(100));
        BigDecimal monthlyAmount = BigDecimal.valueOf(1000);

        // when
        BacktestResult result = backtestEngine.runDCA(data, weights, monthlyAmount);

        // then
        assertThat(result.dailyResults()).hasSize(2);
        // Month 1: 1000 invested -> 10 shares, value 1000
        assertThat(result.dailyResults().get(0).totalValue()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        // Month 2: 10 shares worth 2000 + 1000 new investment (5 shares) -> total 15 shares, value 3000
        assertThat(result.dailyResults().get(1).totalValue()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        // Total invested: 2000. Total value: 3000. Return: 1000 / 2000 * 100 = 50%
        assertThat(result.dailyResults().get(1).returnRate()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }
}
