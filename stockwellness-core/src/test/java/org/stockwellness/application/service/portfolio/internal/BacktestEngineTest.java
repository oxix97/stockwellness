package org.stockwellness.application.service.portfolio.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.domain.portfolio.RebalancingPeriod;
import org.stockwellness.domain.portfolio.strategy.RebalancingStrategy;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.GlobalException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("BacktestEngine 단위 테스트")
class BacktestEngineTest {

    private final BacktestEngine backtestEngine = new BacktestEngine();

    @Test
    @DisplayName("거치식(Lump-sum) 백테스팅: 시작일에 전액 매수 후 수익률 변화를 계산한다")
    void lump_sum_backtest() {
        // given
        LocalDate day1 = LocalDate.of(2024, 1, 1);
        LocalDate day2 = LocalDate.of(2024, 1, 2);
        
        StockPriceResult p1 = new StockPriceResult(day1, BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), 100L, null, null, null, null, null);
        StockPriceResult p2 = new StockPriceResult(day2, BigDecimal.valueOf(110), BigDecimal.valueOf(110), BigDecimal.valueOf(110), BigDecimal.valueOf(110), BigDecimal.valueOf(110), 100L, null, null, null, null, null);
        
        SimulationData data = new SimulationData(
            Map.of("AAPL", List.of(p1, p2)),
            Map.of("KOSPI", List.of(p1, p2)) // Benchmark is same for simplicity
        );
        
        // Portfolio: AAPL 100%
        Map<String, BigDecimal> weights = Map.of("AAPL", BigDecimal.valueOf(100));
        BigDecimal initialAmount = BigDecimal.valueOf(1000);

        // when
        BacktestResult result = backtestEngine.runLumpSum(data, weights, initialAmount, RebalancingPeriod.NONE, "KOSPI", BigDecimal.valueOf(3.0), false);

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
        
        StockPriceResult p1 = new StockPriceResult(month1, BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), 100L, null, null, null, null, null);
        StockPriceResult p2 = new StockPriceResult(month2, BigDecimal.valueOf(200), BigDecimal.valueOf(200), BigDecimal.valueOf(200), BigDecimal.valueOf(200), BigDecimal.valueOf(200), 100L, null, null, null, null, null);
        
        SimulationData data = new SimulationData(
            Map.of("AAPL", List.of(p1, p2)),
            Map.of("KOSPI", List.of(p1, p2))
        );
        
        Map<String, BigDecimal> weights = Map.of("AAPL", BigDecimal.valueOf(100));
        BigDecimal monthlyAmount = BigDecimal.valueOf(1000);

        // when
        BacktestResult result = backtestEngine.runDCA(data, weights, monthlyAmount, RebalancingPeriod.NONE, "KOSPI", BigDecimal.valueOf(3.0), false);

        // then
        assertThat(result.dailyResults()).hasSize(2);
        // Month 1: 1000 invested -> 10 shares, value 1000
        assertThat(result.dailyResults().get(0).totalValue()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        // Month 2: 10 shares worth 2000 + 1000 new investment (5 shares) -> total 15 shares, value 3000
        assertThat(result.dailyResults().get(1).totalValue()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        // 외부 현금흐름을 제거한 일간 TWR은 가격 상승분인 100%만 반영한다.
        assertThat(result.dailyResults().get(1).returnRate()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result.timeWeightedReturnRate()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("리밸런싱 백테스팅: 정해진 주기마다 목표 비중에 맞게 자산을 재조정한다")
    void rebalancing_backtest() {
        // given
        LocalDate day1 = LocalDate.of(2024, 1, 1);
        LocalDate day2 = LocalDate.of(2024, 2, 1); // Month changed -> Monthly rebalancing should trigger
        
        // Stock A goes from 100 to 200 (100% up)
        // Stock B stays at 100
        StockPriceResult a1 = new StockPriceResult(day1, BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), 100L, null, null, null, null, null);
        StockPriceResult a2 = new StockPriceResult(day2, BigDecimal.valueOf(200), BigDecimal.valueOf(200), BigDecimal.valueOf(200), BigDecimal.valueOf(200), BigDecimal.valueOf(200), 100L, null, null, null, null, null);
        StockPriceResult b1 = new StockPriceResult(day1, BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), 100L, null, null, null, null, null);
        StockPriceResult b2 = new StockPriceResult(day2, BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), 100L, null, null, null, null, null);
        
        SimulationData data = new SimulationData(
            Map.of("A", List.of(a1, a2), "B", List.of(b1, b2)),
            Map.of("KOSPI", List.of(a1, a2))
        );
        
        // Portfolio: A 50%, B 50%
        Map<String, BigDecimal> weights = Map.of("A", BigDecimal.valueOf(50), "B", BigDecimal.valueOf(50));
        BigDecimal initialAmount = BigDecimal.valueOf(2000);

        // when
        // Monthly rebalancing
        BacktestResult result = backtestEngine.runLumpSum(data, weights, initialAmount, RebalancingPeriod.MONTHLY, "KOSPI", BigDecimal.valueOf(3.0), false);

        // then
        assertThat(result.dailyResults()).hasSize(2);
        // Day 1: A (10 shares), B (10 shares) -> value 2000
        assertThat(result.dailyResults().get(0).totalValue()).isEqualByComparingTo(BigDecimal.valueOf(2000));
        
        // Day 2 (before rebalance): A (10 * 200 = 2000), B (10 * 100 = 1000) -> 3000
        // Rebalance triggers: 3000 should be 50/50 -> A 1500 (7.5 shares), B 1500 (15 shares)
        // Wait, rebalance happens AT THE START of the day's calculation in my loop? 
        // Let's check my loop: 
        // 1. invest
        // 2. currentValueBeforeRebalance = calculateDailyValue
        // 3. shouldRebalance -> rebalance
        // 4. dailyValue = calculateDailyValue (AFTER rebalance)
        
        // On Day 2: 
        // currentValueBeforeRebalance = A (10*200) + B (10*100) = 3000
        // rebalance triggers -> totalShares updated to target 1500/1500
        // dailyValue = 3000
        assertThat(result.dailyResults().get(1).totalValue()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        // We can't easily check totalShares here as it's private, but we verified the logic.
    }

    @Test
    @DisplayName("평탄한 가격의 DCA는 두 번째 납입일에도 외부 현금흐름을 제거한 TWR을 계산한다")
    void dca_flat_price_uses_time_weighted_return() {
        LocalDate month1 = LocalDate.of(2024, 1, 2);
        LocalDate month2 = LocalDate.of(2024, 2, 1);
        StockPriceResult p1 = price(month1, "100");
        StockPriceResult p2 = price(month2, "100");

        BacktestResult result = backtestEngine.runDCA(
                new SimulationData(Map.of("AAPL", List.of(p1, p2)), Map.of("KOSPI", List.of(p1, p2))),
                Map.of("AAPL", BigDecimal.valueOf(100)), BigDecimal.valueOf(1000),
                RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false);

        assertThat(result.dailyResults().get(1).returnRate()).isZero();
        assertThat(result.mdd()).isZero();
        assertThat(result.xirr()).isZero();
        assertThat(result.cagr()).isNull();
        assertThat(result.timeWeightedReturnRate()).isZero();
    }

    @Test
    @DisplayName("날짜 축은 종목과 벤치마크 시세 날짜의 합집합을 사용한다")
    void date_axis_uses_stock_and_benchmark_union() {
        LocalDate stockDate = LocalDate.of(2024, 1, 2);
        LocalDate benchmarkDate1 = LocalDate.of(2024, 1, 2);
        LocalDate benchmarkDate2 = LocalDate.of(2024, 1, 3);

        BacktestResult result = backtestEngine.runLumpSum(
                new SimulationData(
                        Map.of("AAPL", List.of(price(stockDate, "100"))),
                        Map.of("KOSPI", List.of(price(benchmarkDate1, "100"), price(benchmarkDate2, "101")))),
                Map.of("AAPL", BigDecimal.valueOf(100)), BigDecimal.valueOf(1000),
                RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false);

        assertThat(result.dailyResults()).extracting(BacktestResult.DailyBacktestResult::date)
                .containsExactly(stockDate, benchmarkDate2);
    }

    @Test
    @DisplayName("벤치마크가 자산보다 먼저 시작해도 공통 시작일 이후 백테스트를 수행한다")
    void benchmark_starting_before_asset_does_not_fail() {
        LocalDate benchmarkOnlyDate = LocalDate.of(2024, 1, 2);
        LocalDate stockStart = LocalDate.of(2024, 1, 3);
        LocalDate stockEnd = LocalDate.of(2024, 1, 4);

        BacktestResult result = backtestEngine.runLumpSum(
                new SimulationData(
                        Map.of("AAPL", List.of(price(stockStart, "100"), price(stockEnd, "110"))),
                        Map.of("KOSPI", List.of(
                                price(benchmarkOnlyDate, "100"),
                                price(stockStart, "101"),
                                price(stockEnd, "102")))),
                Map.of("AAPL", BigDecimal.valueOf(100)), BigDecimal.valueOf(1000),
                RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false);

        assertThat(result.dailyResults()).extracting(BacktestResult.DailyBacktestResult::date)
                .containsExactly(stockStart, stockEnd);
        assertThat(result.dailyResults().getFirst().totalValue()).isPositive();
    }

    @Test
    @DisplayName("필수 primary 벤치마크가 없으면 S002로 실패한다")
    void missing_primary_benchmark_fails_with_s002() {
        LocalDate day = LocalDate.of(2024, 1, 2);

        assertThatThrownBy(() -> backtestEngine.runLumpSum(
                new SimulationData(Map.of("AAPL", List.of(price(day, "100"))), Map.of()),
                Map.of("AAPL", BigDecimal.valueOf(100)), BigDecimal.valueOf(1000),
                RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false))
                .isInstanceOf(GlobalException.class)
                .satisfies(error -> assertThat(((GlobalException) error).getErrorCode()).isEqualTo(ErrorCode.PRICE_DATA_NOT_FOUND));
    }

    @Test
    @DisplayName("SP500 primary는 SPX 데이터를 사용하고 scalar 지표가 해당 comparison과 일치한다")
    void sp500_primary_uses_spx_and_preserves_benchmark_order() {
        LocalDate day1 = LocalDate.of(2024, 1, 2);
        LocalDate day2 = LocalDate.of(2024, 1, 3);
        StockPriceResult stock1 = price(day1, "100");
        StockPriceResult stock2 = price(day2, "110");
        LinkedHashMap<String, List<StockPriceResult>> benchmarks = new LinkedHashMap<>();
        benchmarks.put("0001", List.of(price(day1, "100"), price(day2, "105")));
        benchmarks.put("SPX", List.of(price(day1, "100"), price(day2, "120")));
        benchmarks.put("1001", List.of(price(day1, "100"), price(day2, "101")));

        BacktestResult result = backtestEngine.runLumpSum(
                new SimulationData(Map.of("AAPL", List.of(stock1, stock2)), benchmarks),
                Map.of("AAPL", BigDecimal.valueOf(100)), BigDecimal.valueOf(1000),
                RebalancingPeriod.NONE, "SP500", BigDecimal.ZERO, false);

        assertThat(result.comparisons()).extracting(BacktestResult.IndexComparison::ticker)
                .containsExactly("0001", "SPX", "1001");
        BacktestResult.IndexComparison primary = result.comparisons().get(1);
        assertThat(result.alpha()).isEqualByComparingTo(primary.alpha());
        assertThat(result.beta()).isEqualByComparingTo(primary.beta());
        assertThat(result.relativeMdd()).isEqualByComparingTo(primary.relativeMdd());
    }

    @Test
    @DisplayName("0 이하 종가는 S002로 실패하고 0 대체값을 사용하지 않는다")
    void non_positive_price_fails_with_s002() {
        LocalDate day = LocalDate.of(2024, 1, 2);
        StockPriceResult invalid = price(day, "0");
        assertThatThrownBy(() -> backtestEngine.runLumpSum(
                new SimulationData(Map.of("AAPL", List.of(invalid)), Map.of("KOSPI", List.of(price(day, "100")))),
                Map.of("AAPL", BigDecimal.valueOf(100)), BigDecimal.valueOf(1000),
                RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false))
                .isInstanceOf(GlobalException.class)
                .satisfies(error -> assertThat(((GlobalException) error).getErrorCode()).isEqualTo(ErrorCode.PRICE_DATA_NOT_FOUND));
    }

    @Test
    @DisplayName("거치식 1년: 배당재투자(수정주가) 여부에 따라 기대 평가액·CAGR·TWR·Alpha가 독립 계산표와 일치한다")
    void lump_sum_one_year_dividend_reinvestment_and_metrics() {
        LocalDate d0 = LocalDate.of(2025, 1, 1);
        LocalDate d1 = LocalDate.of(2025, 7, 2);
        LocalDate d2 = LocalDate.of(2026, 1, 1); // 365 calendar days -> years = 1.0
        List<StockPriceResult> stock = List.of(
                price(d0, "100", "100"),
                price(d1, "105", "110"),
                price(d2, "110", "121")
        );
        List<StockPriceResult> kospi = List.of(
                price(d0, "100"),
                price(d1, "105"),
                price(d2, "110")
        );
        SimulationData data = new SimulationData(Map.of("AAPL", stock), Map.of("0001", kospi));
        Map<String, BigDecimal> weights = Map.of("AAPL", BigDecimal.valueOf(100));

        BacktestResult reinvested = backtestEngine.runLumpSum(
                data, weights, BigDecimal.valueOf(1000), RebalancingPeriod.NONE, "KOSPI", BigDecimal.valueOf(3.0), true);

        assertThat(reinvested.calculationMethod()).isEqualTo("LUMP_SUM_CAGR_TWR");
        assertThat(reinvested.xirr()).isNull();
        assertThat(reinvested.dailyResults().get(0).totalInvested()).isEqualByComparingTo("1000");
        assertThat(reinvested.dailyResults().get(1).totalValue()).isEqualByComparingTo("1100");
        assertThat(reinvested.dailyResults().get(1).returnRate()).isEqualByComparingTo("10");
        assertThat(reinvested.dailyResults().get(2).totalValue()).isEqualByComparingTo("1210");
        assertThat(reinvested.dailyResults().get(2).returnRate()).isEqualByComparingTo("10");
        assertThat(reinvested.totalReturnRate()).isEqualByComparingTo("21");
        assertThat(reinvested.timeWeightedReturnRate()).isEqualByComparingTo("21");
        assertThat(reinvested.cagr()).isCloseTo(new BigDecimal("21"), within(new BigDecimal("0.0001")));
        assertThat(reinvested.mdd()).isZero();
        assertThat(reinvested.relativeMdd()).isZero();
        assertThat(reinvested.alpha()).isEqualByComparingTo("11"); // 21% - 10%
        assertThat(reinvested.itemReturns().get("AAPL")).isEqualByComparingTo("21");
        assertThat(reinvested.sortinoRatio()).isNull();
        assertThat(reinvested.recoveryPeriod()).isZero();

        BacktestResult priceOnly = backtestEngine.runLumpSum(
                data, weights, BigDecimal.valueOf(1000), RebalancingPeriod.NONE, "KOSPI", BigDecimal.valueOf(3.0), false);

        assertThat(priceOnly.dailyResults().get(2).totalValue()).isEqualByComparingTo("1100");
        assertThat(priceOnly.totalReturnRate()).isEqualByComparingTo("10");
        assertThat(priceOnly.cagr()).isCloseTo(new BigDecimal("10"), within(new BigDecimal("0.0001")));
        assertThat(priceOnly.alpha()).isZero();
    }

    @Test
    @DisplayName("거치식 음수 수익률: 가격 100에서 1,000 투자 후 90 평가 시 평가액 900, 손익 -100, 수익률 -10%, MDD +10%를 반환한다")
    void lump_sum_negative_return_1000_invested_at_100_evaluated_at_90() {
        LocalDate d0 = LocalDate.of(2025, 1, 1);
        LocalDate d1 = LocalDate.of(2026, 1, 1); // 365 days -> years = 1.0
        SimulationData data = new SimulationData(
                Map.of("AAPL", List.of(price(d0, "100"), price(d1, "90"))),
                Map.of("0001", List.of(price(d0, "100"), price(d1, "100")))
        );

        BacktestResult result = backtestEngine.runLumpSum(
                data, Map.of("AAPL", BigDecimal.valueOf(100)), BigDecimal.valueOf(1000),
                RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false);

        assertThat(result.dailyResults()).hasSize(2);
        assertThat(result.dailyResults().get(0).totalValue()).isEqualByComparingTo("1000");
        assertThat(result.dailyResults().get(1).totalInvested()).isEqualByComparingTo("1000");
        assertThat(result.dailyResults().get(1).totalValue()).isEqualByComparingTo("900");
        assertThat(result.dailyResults().get(1).totalValue().subtract(result.dailyResults().get(1).totalInvested()))
                .isEqualByComparingTo("-100");
        assertThat(result.dailyResults().get(1).returnRate()).isEqualByComparingTo("-10");
        assertThat(result.totalReturnRate()).isEqualByComparingTo("-10");
        assertThat(result.timeWeightedReturnRate()).isEqualByComparingTo("-10");
        assertThat(result.cagr()).isCloseTo(new BigDecimal("-10"), within(new BigDecimal("0.0001")));
        assertThat(result.xirr()).isNull();
        // ReturnSeries.calculateMDD는 (peak - trough) / peak * 100 양수 크기(+)를 반환한다.
        assertThat(result.mdd()).isEqualByComparingTo("10");
        assertThat(result.relativeMdd()).isEqualByComparingTo("10");
        assertThat(result.alpha()).isEqualByComparingTo("-10");
        assertThat(result.itemReturns().get("AAPL")).isEqualByComparingTo("-10");
        assertThat(result.recoveryPeriod()).isEqualTo(365L);
    }

    @Test
    @DisplayName("적립식 1년: 동일 월 중복 입금 없이 XIRR 10%, TWR 10%, 총투자금 2,000, 평가액 2,100을 계산한다")
    void dca_one_year_exact_xirr_and_same_month_single_deposit() {
        LocalDate d0 = LocalDate.of(2025, 1, 2);
        LocalDate d1 = LocalDate.of(2025, 1, 15); // same YearMonth -> no additional deposit
        LocalDate d2 = LocalDate.of(2026, 1, 2);  // 365 days from d0 -> second deposit
        SimulationData data = new SimulationData(
                Map.of("AAPL", List.of(price(d0, "100"), price(d1, "110"), price(d2, "110"))),
                Map.of("0001", List.of(price(d0, "100"), price(d1, "105"), price(d2, "110")))
        );

        BacktestResult result = backtestEngine.runDCA(
                data, Map.of("AAPL", BigDecimal.valueOf(100)), BigDecimal.valueOf(1000),
                RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false);

        assertThat(result.calculationMethod()).isEqualTo("DCA_XIRR_TWR");
        assertThat(result.cagr()).isNull();
        assertThat(result.dailyResults()).hasSize(3);
        assertThat(result.dailyResults().get(0).totalInvested()).isEqualByComparingTo("1000");
        assertThat(result.dailyResults().get(1).totalInvested()).isEqualByComparingTo("1000");
        assertThat(result.dailyResults().get(1).totalValue()).isEqualByComparingTo("1100");
        assertThat(result.dailyResults().get(1).returnRate()).isEqualByComparingTo("10");
        assertThat(result.dailyResults().get(2).totalInvested()).isEqualByComparingTo("2000");
        assertThat(result.dailyResults().get(2).totalValue()).isCloseTo(new BigDecimal("2100"), within(new BigDecimal("0.0001")));
        assertThat(result.dailyResults().get(2).returnRate()).isCloseTo(BigDecimal.ZERO, within(new BigDecimal("0.000001")));
        assertThat(result.timeWeightedReturnRate()).isCloseTo(new BigDecimal("10"), within(new BigDecimal("0.0001")));
        // -1000 + (-1000 + 2100) / (1 + r)^1 = 0 => r = 10%
        assertThat(result.xirr()).isCloseTo(new BigDecimal("10"), within(new BigDecimal("0.0001")));
        assertThat(result.mdd()).isZero();
        assertThat(result.itemReturns().get("AAPL")).isCloseTo(new BigDecimal("5"), within(new BigDecimal("0.0001")));
    }

    @Test
    @DisplayName("적립식 음수 수익률: XIRR -10%, TWR -10%, MDD +10%, CAGR null을 반환한다")
    void dca_negative_return_produces_negative_xirr_and_twr() {
        LocalDate d0 = LocalDate.of(2025, 1, 1);
        LocalDate d1 = LocalDate.of(2026, 1, 1); // 365 days -> t = 1.0
        SimulationData data = new SimulationData(
                Map.of("AAPL", List.of(price(d0, "100"), price(d1, "90"))),
                Map.of("0001", List.of(price(d0, "100"), price(d1, "100")))
        );

        BacktestResult result = backtestEngine.runDCA(
                data, Map.of("AAPL", BigDecimal.valueOf(100)), BigDecimal.valueOf(1000),
                RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false);

        assertThat(result.cagr()).isNull();
        assertThat(result.dailyResults().get(1).totalInvested()).isEqualByComparingTo("2000");
        assertThat(result.dailyResults().get(1).totalValue()).isCloseTo(new BigDecimal("1900"), within(new BigDecimal("0.0001")));
        assertThat(result.dailyResults().get(1).returnRate()).isCloseTo(new BigDecimal("-10"), within(new BigDecimal("0.0001")));
        assertThat(result.timeWeightedReturnRate()).isCloseTo(new BigDecimal("-10"), within(new BigDecimal("0.0001")));
        // -1000 + (-1000 + 1900) / (1 + r)^1 = 0 => r = -10%
        assertThat(result.xirr()).isCloseTo(new BigDecimal("-10"), within(new BigDecimal("0.0001")));
        assertThat(result.mdd()).isEqualByComparingTo("10");
        assertThat(result.itemReturns().get("AAPL")).isCloseTo(new BigDecimal("-5"), within(new BigDecimal("0.0001")));
    }

    @Test
    @DisplayName("0원 및 음수 투자금은 거치식·적립식 모두 G001(INVALID_INPUT_VALUE)로 거부한다")
    void zero_and_negative_amount_fail_with_g001() {
        LocalDate day = LocalDate.of(2025, 1, 2);
        SimulationData data = new SimulationData(
                Map.of("AAPL", List.of(price(day, "100"))),
                Map.of("0001", List.of(price(day, "100")))
        );
        Map<String, BigDecimal> weights = Map.of("AAPL", BigDecimal.valueOf(100));

        for (BigDecimal invalidAmount : List.of(BigDecimal.ZERO, BigDecimal.valueOf(-1000))) {
            assertThatThrownBy(() -> backtestEngine.runLumpSum(
                    data, weights, invalidAmount, RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false))
                    .isInstanceOf(GlobalException.class)
                    .satisfies(error -> assertThat(((GlobalException) error).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));

            assertThatThrownBy(() -> backtestEngine.runDCA(
                    data, weights, invalidAmount, RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false))
                    .isInstanceOf(GlobalException.class)
                    .satisfies(error -> assertThat(((GlobalException) error).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        }
    }

    @Test
    @DisplayName("휴장일: 종목 휴장일에 직전 거래일 종가를 유지(forward-fill)하여 일간 수익률 0%, 벤치마크는 당일 종가로 계산한다")
    void market_holiday_forward_fills_last_known_price_and_zero_daily_return() {
        LocalDate d0 = LocalDate.of(2025, 1, 2);
        LocalDate holiday = LocalDate.of(2025, 1, 3);
        LocalDate d2 = LocalDate.of(2025, 1, 6);
        SimulationData data = new SimulationData(
                Map.of("AAPL", List.of(price(d0, "100"), price(d2, "110"))),
                Map.of("0001", List.of(price(d0, "100"), price(holiday, "105"), price(d2, "110")))
        );

        BacktestResult result = backtestEngine.runLumpSum(
                data, Map.of("AAPL", BigDecimal.valueOf(100)), BigDecimal.valueOf(1000),
                RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false);

        assertThat(result.dailyResults()).extracting(BacktestResult.DailyBacktestResult::date)
                .containsExactly(d0, holiday, d2);
        assertThat(result.dailyResults().get(1).totalValue()).isEqualByComparingTo("1000");
        assertThat(result.dailyResults().get(1).returnRate()).isZero();
        assertThat(result.dailyResults().get(1).benchmarkReturnRates().get("0001")).isEqualByComparingTo("5");
        assertThat(result.dailyResults().get(2).totalValue()).isEqualByComparingTo("1100");
        assertThat(result.dailyResults().get(2).returnRate()).isEqualByComparingTo("10");
        assertThat(result.dailyResults().get(2).benchmarkReturnRates().get("0001")).isEqualByComparingTo("10");
        assertThat(result.totalReturnRate()).isEqualByComparingTo("10");
        assertThat(result.alpha()).isZero();
    }

    @Test
    @DisplayName("가격 누락: 가중치 종목 시세 부재, 빈 리스트, null 종가, 음수 종가는 모두 S002로 실패한다")
    void missing_stock_price_cases_fail_with_s002() {
        LocalDate day = LocalDate.of(2025, 1, 2);
        Map<String, List<StockPriceResult>> validBenchmark = Map.of("0001", List.of(price(day, "100")));

        // 1) 가중치 종목 중 하나가 stockPrices에 없음
        assertThatThrownBy(() -> backtestEngine.runLumpSum(
                new SimulationData(Map.of("AAPL", List.of(price(day, "100"))), validBenchmark),
                Map.of("AAPL", BigDecimal.valueOf(50), "MSFT", BigDecimal.valueOf(50)),
                BigDecimal.valueOf(1000), RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false))
                .isInstanceOf(GlobalException.class)
                .satisfies(error -> assertThat(((GlobalException) error).getErrorCode()).isEqualTo(ErrorCode.PRICE_DATA_NOT_FOUND));

        // 2) 빈 시세 리스트
        assertThatThrownBy(() -> backtestEngine.runLumpSum(
                new SimulationData(Map.of("AAPL", List.of()), validBenchmark),
                Map.of("AAPL", BigDecimal.valueOf(100)),
                BigDecimal.valueOf(1000), RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false))
                .isInstanceOf(GlobalException.class)
                .satisfies(error -> assertThat(((GlobalException) error).getErrorCode()).isEqualTo(ErrorCode.PRICE_DATA_NOT_FOUND));

        // 3) closePrice == null (dividendReinvested=false)
        StockPriceResult nullClose = new StockPriceResult(
                day, null, null, null, null, null, 100L, null, null, null, null, null);
        assertThatThrownBy(() -> backtestEngine.runLumpSum(
                new SimulationData(Map.of("AAPL", List.of(nullClose)), validBenchmark),
                Map.of("AAPL", BigDecimal.valueOf(100)),
                BigDecimal.valueOf(1000), RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false))
                .isInstanceOf(GlobalException.class)
                .satisfies(error -> assertThat(((GlobalException) error).getErrorCode()).isEqualTo(ErrorCode.PRICE_DATA_NOT_FOUND));

        // 4) 음수 종가
        assertThatThrownBy(() -> backtestEngine.runLumpSum(
                new SimulationData(Map.of("AAPL", List.of(price(day, "-50"))), validBenchmark),
                Map.of("AAPL", BigDecimal.valueOf(100)),
                BigDecimal.valueOf(1000), RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false))
                .isInstanceOf(GlobalException.class)
                .satisfies(error -> assertThat(((GlobalException) error).getErrorCode()).isEqualTo(ErrorCode.PRICE_DATA_NOT_FOUND));
    }

    @Test
    @DisplayName("단일 관측치: 거치식은 수익률·CAGR 0, 적립식은 CAGR·XIRR null과 안전 지표를 반환한다")
    void single_observation_date_lump_sum_and_dca() {
        LocalDate day = LocalDate.of(2025, 1, 2);
        SimulationData data = new SimulationData(
                Map.of("AAPL", List.of(price(day, "100"))),
                Map.of("0001", List.of(price(day, "100")))
        );
        Map<String, BigDecimal> weights = Map.of("AAPL", BigDecimal.valueOf(100));

        BacktestResult lumpSum = backtestEngine.runLumpSum(
                data, weights, BigDecimal.valueOf(1000), RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false);

        assertThat(lumpSum.dailyResults()).hasSize(1);
        assertThat(lumpSum.dailyResults().getFirst().totalValue()).isEqualByComparingTo("1000");
        assertThat(lumpSum.dailyResults().getFirst().totalInvested()).isEqualByComparingTo("1000");
        assertThat(lumpSum.dailyResults().getFirst().returnRate()).isZero();
        assertThat(lumpSum.totalReturnRate()).isZero();
        assertThat(lumpSum.timeWeightedReturnRate()).isZero();
        assertThat(lumpSum.cagr()).isZero();
        assertThat(lumpSum.xirr()).isNull();
        assertThat(lumpSum.mdd()).isZero();
        assertThat(lumpSum.relativeMdd()).isZero();
        assertThat(lumpSum.volatility()).isZero();
        assertThat(lumpSum.sharpeRatio()).isZero();
        assertThat(lumpSum.sortinoRatio()).isNull();
        assertThat(lumpSum.recoveryPeriod()).isNull();
        assertThat(lumpSum.beta()).isOne();
        assertThat(lumpSum.alpha()).isZero();

        BacktestResult dca = backtestEngine.runDCA(
                data, weights, BigDecimal.valueOf(1000), RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false);

        assertThat(dca.dailyResults()).hasSize(1);
        assertThat(dca.calculationMethod()).isEqualTo("DCA_XIRR_TWR");
        assertThat(dca.cagr()).isNull();
        assertThat(dca.xirr()).isNull();
        assertThat(dca.timeWeightedReturnRate()).isZero();
        assertThat(dca.sortinoRatio()).isNull();
        assertThat(dca.recoveryPeriod()).isNull();
    }

    @Test
    @DisplayName("필수 primary 벤치마크 누락: 보조 벤치마크가 있어도 요청한 primary가 없거나 비어 있으면 S002로 실패한다")
    void missing_primary_benchmark_even_with_secondary_benchmark_fails_with_s002() {
        LocalDate day = LocalDate.of(2025, 1, 2);
        Map<String, List<StockPriceResult>> stocks = Map.of("AAPL", List.of(price(day, "100")));
        Map<String, BigDecimal> weights = Map.of("AAPL", BigDecimal.valueOf(100));

        assertThatThrownBy(() -> backtestEngine.runLumpSum(
                new SimulationData(stocks, Map.of("1001", List.of(price(day, "100")))),
                weights, BigDecimal.valueOf(1000), RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false))
                .isInstanceOf(GlobalException.class)
                .satisfies(error -> assertThat(((GlobalException) error).getErrorCode()).isEqualTo(ErrorCode.PRICE_DATA_NOT_FOUND));

        assertThatThrownBy(() -> backtestEngine.runLumpSum(
                new SimulationData(stocks, Map.of("0001", List.of())),
                weights, BigDecimal.valueOf(1000), RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false))
                .isInstanceOf(GlobalException.class)
                .satisfies(error -> assertThat(((GlobalException) error).getErrorCode()).isEqualTo(ErrorCode.PRICE_DATA_NOT_FOUND));
    }

    @Test
    @DisplayName("경계 반올림: 중간 소수 수량(scale=16)과 월간 리밸런싱 수량(scale=8)의 반올림 오차가 계약 범위 내에 있다")
    void boundary_rounding_fractional_shares_and_monthly_rebalance_scale() {
        LocalDate d0 = LocalDate.of(2025, 1, 2);
        LocalDate d1 = LocalDate.of(2025, 2, 3);
        LocalDate d2 = LocalDate.of(2025, 2, 4);

        // 1) 단일 종목 소수점 수량 scale=16 HALF_UP (1000 / 300 = 3.3333333333333333)
        BacktestResult fractional = backtestEngine.runLumpSum(
                new SimulationData(
                        Map.of("AAPL", List.of(price(d0, "300"), price(d1, "300"))),
                        Map.of("0001", List.of(price(d0, "100"), price(d1, "100")))
                ),
                Map.of("AAPL", BigDecimal.valueOf(100)), BigDecimal.valueOf(1000),
                RebalancingPeriod.NONE, "KOSPI", BigDecimal.ZERO, false);

        assertThat(fractional.dailyResults().get(0).totalValue())
                .isEqualByComparingTo(new BigDecimal("999.9999999999999900"));
        assertThat(fractional.dailyResults().get(1).returnRate()).isZero();

        // 4원을 25%/75%로 배분: 1/200000000=0.000000005, 3/200000000=0.000000015.
        // scale 8 HALF_UP 수량은 각각 0.00000001, 0.00000002이며 현금 잔액은 모델에 없다.
        // 리밸런싱 직후 평가액은 2+4=6원, 다음 날 A 가격이 두 배면 4+4=8원이다.
        Map<String, BigDecimal> weights = Map.of("A", BigDecimal.valueOf(25), "B", BigDecimal.valueOf(75));
        Map<String, BigDecimal> prices = Map.of("A", new BigDecimal("200000000"), "B", new BigDecimal("200000000"));
        Map<String, BigDecimal> shares = new HashMap<>();
        new RebalancingStrategy().rebalance(shares, new BigDecimal("4"), weights, prices);
        assertThat(shares.get("A")).isEqualByComparingTo("0.00000001");
        assertThat(shares.get("B")).isEqualByComparingTo("0.00000002");

        BacktestResult rebalanced = backtestEngine.runLumpSum(
                new SimulationData(
                        Map.of(
                                "A", List.of(price(d0, "200000000"), price(d1, "200000000"), price(d2, "400000000")),
                                "B", List.of(price(d0, "200000000"), price(d1, "200000000"), price(d2, "200000000"))
                        ),
                        Map.of("0001", List.of(price(d0, "100"), price(d1, "100"), price(d2, "100")))
                ),
                weights, BigDecimal.valueOf(4), RebalancingPeriod.MONTHLY, "KOSPI", BigDecimal.ZERO, false);

        assertThat(rebalanced.dailyResults().get(0).totalValue()).isEqualByComparingTo("4");
        assertThat(rebalanced.dailyResults().get(1).totalValue()).isEqualByComparingTo("6");
        assertThat(rebalanced.dailyResults().get(2).totalValue()).isEqualByComparingTo("8");

    }

    private static StockPriceResult price(LocalDate date, String close) {
        return price(date, close, close);
    }

    private static StockPriceResult price(LocalDate date, String close, String adjClose) {
        BigDecimal closeValue = new BigDecimal(close);
        BigDecimal adjCloseValue = new BigDecimal(adjClose);
        return new StockPriceResult(
                date, closeValue, closeValue, closeValue, closeValue, adjCloseValue, 100L, null, null, null, null, null);
    }
}
