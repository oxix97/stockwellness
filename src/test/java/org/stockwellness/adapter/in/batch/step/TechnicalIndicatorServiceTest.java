package org.stockwellness.adapter.in.batch.step;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.stock.StockHistory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TechnicalIndicatorServiceTest {

    private final TechnicalIndicatorService service = new TechnicalIndicatorService();

    // -------------------------------------------------------------------------
    // 1. 이동평균선(MA) 검증
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("MA5: 데이터가 4개(5일 미만)일 때는 Null이어야 한다 (데이터 부족)")
    void calculate_ma5_insufficient_data() {
        // Given
        // 과거 3일치 + 오늘 1일치 = 총 4개
        List<StockHistory> past = new ArrayList<>();
        past.add(createTestStock(LocalDate.now().minusDays(3), 1000));
        past.add(createTestStock(LocalDate.now().minusDays(2), 1000));
        past.add(createTestStock(LocalDate.now().minusDays(1), 1000));

        StockHistory today = createTestStock(LocalDate.now(), 1000);

        // When
        service.calculateAndFill(today, past);

        // Then
        assertThat(today.getMa5()).isNull();
    }

    @Test
    @DisplayName("MA5: 데이터가 5개일 때 정확한 평균값이 계산되어야 한다")
    void calculate_ma5_exact() {
        // Given: 과거 100, 200, 300, 400
        List<StockHistory> past = new ArrayList<>();
        past.add(createTestStock(LocalDate.now().minusDays(4), 100));
        past.add(createTestStock(LocalDate.now().minusDays(3), 200));
        past.add(createTestStock(LocalDate.now().minusDays(2), 300));
        past.add(createTestStock(LocalDate.now().minusDays(1), 400));

        // 오늘 500
        StockHistory today = createTestStock(LocalDate.now(), 500);

        // When
        service.calculateAndFill(today, past);

        // Then: (100+200+300+400+500) / 5 = 300
        assertThat(today.getMa5()).isNotNull();
        assertThat(today.getMa5()).isEqualByComparingTo(BigDecimal.valueOf(300));
    }

    // -------------------------------------------------------------------------
    // 2. RSI (상대강도지수) 검증
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RSI14: 지속적인 상승장(Uptrend)에서는 RSI가 70을 초과해야 한다")
    void calculate_rsi_uptrend() {
        // Given: 20일 동안 매일 100원씩 상승
        List<StockHistory> past = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(20);

        for (int i = 0; i < 19; i++) {
            // 1000원부터 시작해서 매일 100원씩 상승
            past.add(createTestStock(start.plusDays(i), 1000 + (i * 100)));
        }
        StockHistory today = createTestStock(LocalDate.now(), 1000 + (19 * 100));

        // When
        service.calculateAndFill(today, past);

        // Then
        assertThat(today.getRsi14()).isNotNull();
        System.out.println("Calculated Uptrend RSI: " + today.getRsi14());
        assertThat(today.getRsi14()).isGreaterThan(BigDecimal.valueOf(70));
    }

    @Test
    @DisplayName("RSI14: 지속적인 하락장(Downtrend)에서는 RSI가 30 미만이어야 한다")
    void calculate_rsi_downtrend() {
        // Given: 20일 동안 매일 100원씩 하락
        List<StockHistory> past = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(20);

        for (int i = 0; i < 19; i++) {
            // 5000원부터 시작해서 매일 100원씩 하락
            past.add(createTestStock(start.plusDays(i), 5000 - (i * 100)));
        }
        StockHistory today = createTestStock(LocalDate.now(), 5000 - (19 * 100));

        // When
        service.calculateAndFill(today, past);

        // Then
        assertThat(today.getRsi14()).isNotNull();
        System.out.println("Calculated Downtrend RSI: " + today.getRsi14());
        assertThat(today.getRsi14()).isLessThan(BigDecimal.valueOf(30));
    }

    // -------------------------------------------------------------------------
    // 🛠️ Test Helper Method (Factory Method 사용)
    // -------------------------------------------------------------------------
    private StockHistory createTestStock(LocalDate date, double price) {
        BigDecimal val = BigDecimal.valueOf(price);

        // 사용자가 제공한 static create 메소드 사용
        // 테스트에 불필요한 값(거래량, 시가총액 등)은 0 또는 null로 채움
        return StockHistory.create(
                "KR005930",         // isinCode (Dummy)
                date,               // baseDate
                val,                // closePrice (핵심)
                val,                // openPrice (Ta4j 호환용)
                val,                // highPrice (Ta4j 호환용)
                val,                // lowPrice  (Ta4j 호환용)
                BigDecimal.ZERO,    // priceChange
                BigDecimal.ZERO,    // fluctuationRate
                1000L,              // volume
                BigDecimal.ZERO,    // tradingValue
                BigDecimal.ZERO     // marketCap
        );
    }
}