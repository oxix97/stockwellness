package org.stockwellness.application.service.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.application.port.in.portfolio.result.StockStatResult;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockHistory;
import org.stockwellness.fixture.StockFixture;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StockStatCalculatorTest {

    private final StockStatCalculator stockStatCalculator = new StockStatCalculator();

    @Test
    @DisplayName("방어력(Defense) 점수 계산 - 시가총액 기준")
    void calculateDefenseScore() {
        Stock stock = StockFixture.createStock();
        LocalDate now = LocalDate.now();

        // 100점: 20조 이상
        StockStatResult stat100 = stockStatCalculator.calculate(stock, List.of(createHistoryWithMarketCap(25_000_000_000_000L)));
        // 80점: 5조 ~ 20조
        StockStatResult stat80 = stockStatCalculator.calculate(stock, List.of(createHistoryWithMarketCap(10_000_000_000_000L)));
        // 60점: 1조 ~ 5조
        StockStatResult stat60 = stockStatCalculator.calculate(stock, List.of(createHistoryWithMarketCap(2_000_000_000_000L)));
        // 40점: 1조 미만
        StockStatResult stat40 = stockStatCalculator.calculate(stock, List.of(createHistoryWithMarketCap(500_000_000_000L)));

        assertThat(stat100.defense()).isEqualTo(100);
        assertThat(stat80.defense()).isEqualTo(80);
        assertThat(stat60.defense()).isEqualTo(60);
        assertThat(stat40.defense()).isEqualTo(40);
    }

    @Test
    @DisplayName("공격력(Attack) 점수 계산 - RSI 및 MACD 기준")
    void calculateAttackScore() {
        Stock stock = StockFixture.createStock();

        // RSI 75, MACD 10 -> 90 + 10 = 100
        StockStatResult stat100 = stockStatCalculator.calculate(stock, List.of(createHistoryWithAttack(75, 10)));
        // RSI 60, MACD 5 -> 70 + 10 = 80
        StockStatResult stat80 = stockStatCalculator.calculate(stock, List.of(createHistoryWithAttack(60, 5)));
        // RSI 40, MACD -1 -> 40 + 0 = 40
        StockStatResult stat40 = stockStatCalculator.calculate(stock, List.of(createHistoryWithAttack(40, -5)));

        assertThat(stat100.attack()).isEqualTo(100);
        assertThat(stat80.attack()).isEqualTo(80);
        assertThat(stat40.attack()).isEqualTo(40);
    }

    @Test
    @DisplayName("지구력(Endurance) 점수 계산 - 120일 이격도 기준")
    void calculateEnduranceScore() {
        Stock stock = StockFixture.createStock();

        // 100점: 100% <= Disparity <= 110%
        StockStatResult stat100 = stockStatCalculator.calculate(stock, List.of(createHistoryWithEndurance(10500, 10000))); // 105%
        // 70점: Disparity > 110%
        StockStatResult stat70 = stockStatCalculator.calculate(stock, List.of(createHistoryWithEndurance(12000, 10000))); // 120%
        // 40점: Disparity < 100%
        StockStatResult stat40 = stockStatCalculator.calculate(stock, List.of(createHistoryWithEndurance(9000, 10000))); // 90%

        assertThat(stat100.endurance()).isEqualTo(100);
        assertThat(stat70.endurance()).isEqualTo(70);
        assertThat(stat40.endurance()).isEqualTo(40);
    }

    @Test
    @DisplayName("민첩성(Agility) 점수 계산 - 5일 변동성 기준")
    void calculateAgilityScore() {
        Stock stock = StockFixture.createStock();

        // 100점: Avg > 5%
        List<StockHistory> histories100 = List.of(
                createHistoryWithFlt(6.0), createHistoryWithFlt(-5.0), createHistoryWithFlt(7.0),
                createHistoryWithFlt(-4.0), createHistoryWithFlt(6.0)
        ); // Abs avg: (6+5+7+4+6)/5 = 28/5 = 5.6%
        
        // 70점: 2% ~ 5%
        List<StockHistory> histories70 = List.of(
                createHistoryWithFlt(3.0), createHistoryWithFlt(-2.0), createHistoryWithFlt(4.0),
                createHistoryWithFlt(-1.0), createHistoryWithFlt(2.0)
        ); // Abs avg: (3+2+4+1+2)/5 = 12/5 = 2.4%

        // 40점: < 2%
        List<StockHistory> histories40 = List.of(
                createHistoryWithFlt(1.0), createHistoryWithFlt(-0.5), createHistoryWithFlt(1.5),
                createHistoryWithFlt(-0.2), createHistoryWithFlt(0.8)
        ); // Abs avg: (1+0.5+1.5+0.2+0.8)/5 = 4/5 = 0.8%

        assertThat(stockStatCalculator.calculate(stock, histories100).agility()).isEqualTo(100);
        assertThat(stockStatCalculator.calculate(stock, histories70).agility()).isEqualTo(70);
        assertThat(stockStatCalculator.calculate(stock, histories40).agility()).isEqualTo(40);
    }

    @Test
    @DisplayName("데이터 누락 시 기본값 50점 반환")
    void returnDefaultScoreForMissingData() {
        Stock stock = StockFixture.createStock();
        StockHistory emptyHistory = StockHistory.create(
                stock.getIsinCode(), LocalDate.now(), null, null, null, null,
                null, null, null, null, null
        );

        StockStatResult stat = stockStatCalculator.calculate(stock, List.of(emptyHistory));

        assertThat(stat.defense()).isEqualTo(50);
        assertThat(stat.attack()).isEqualTo(50);
        assertThat(stat.endurance()).isEqualTo(50);
        assertThat(stat.agility()).isEqualTo(50);
    }

    private StockHistory createHistoryWithMarketCap(long marketCap) {
        StockHistory history = StockFixture.createHistory(StockFixture.ISIN_CODE, LocalDate.now(), 10000);
        setMarketCap(history, marketCap);
        return history;
    }

    private StockHistory createHistoryWithAttack(double rsi, double macd) {
        StockHistory history = StockFixture.createHistory(StockFixture.ISIN_CODE, LocalDate.now(), 10000);
        history.updateRsi14(BigDecimal.valueOf(rsi));
        history.updateMacd(BigDecimal.valueOf(macd));
        return history;
    }

    private StockHistory createHistoryWithEndurance(double price, double ma120) {
        StockHistory history = StockHistory.create(
                StockFixture.ISIN_CODE, LocalDate.now(), BigDecimal.valueOf(price),
                BigDecimal.valueOf(price), BigDecimal.valueOf(price), BigDecimal.valueOf(price),
                BigDecimal.ZERO, BigDecimal.ZERO, 100L, BigDecimal.ZERO, BigDecimal.ZERO
        );
        history.updateMa120(BigDecimal.valueOf(ma120));
        return history;
    }

    private StockHistory createHistoryWithFlt(double flt) {
        return StockHistory.create(
                StockFixture.ISIN_CODE, LocalDate.now(), BigDecimal.valueOf(10000),
                BigDecimal.valueOf(10000), BigDecimal.valueOf(10000), BigDecimal.valueOf(10000),
                BigDecimal.ZERO, BigDecimal.valueOf(flt), 100L, BigDecimal.ZERO, BigDecimal.ZERO
        );
    }

    private void setMarketCap(StockHistory history, long marketCap) {
        try {
            java.lang.reflect.Field field = StockHistory.class.getDeclaredField("marketCap");
            field.setAccessible(true);
            field.set(history, BigDecimal.valueOf(marketCap));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}