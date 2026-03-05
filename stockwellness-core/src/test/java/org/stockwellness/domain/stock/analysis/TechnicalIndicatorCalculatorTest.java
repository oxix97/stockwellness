package org.stockwellness.domain.stock.analysis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("단위 테스트: 기술적 지표 계산기 정밀 검증")
class TechnicalIndicatorCalculatorTest {

    // --- Helper Method: 테스트용 더미 데이터 생성기 ---
    private List<BigDecimal> createPriceList(int size, double startPrice, double increment) {
        List<BigDecimal> prices = new ArrayList<>();
        double current = startPrice;
        for (int i = 0; i < size; i++) {
            prices.add(BigDecimal.valueOf(current));
            current += increment;
        }
        return prices;
    }

    @Nested
    @DisplayName("1. 이동평균선(MA) 검증")
    class MovingAverageTest {

        @Test
        @DisplayName("데이터가 충분할 때 MA5, MA20이 정확히 계산된다")
        void calculateMA_standard() {
            // Given: 20일치 데이터 (100, 110, ..., 290)
            List<BigDecimal> prices = createPriceList(20, 100, 10);

            // When
            List<TechnicalIndicators> results = TechnicalIndicatorCalculator.calculateSeries(prices);

            // Then
            assertThat(results.get(4).getMa5())
                    .as("5일차의 MA5는 120이어야 함")
                    .isEqualByComparingTo(BigDecimal.valueOf(120));

            assertThat(results.get(19).getMa20())
                    .as("20일차의 MA20은 195이어야 함")
                    .isEqualByComparingTo(BigDecimal.valueOf(195));
        }

        @Test
        @DisplayName("데이터가 부족하면(MA 기간 미달) null을 반환한다")
        void calculateMA_insufficient_data() {
            // Given: 데이터 4개 (MA5 계산 불가)
            List<BigDecimal> prices = createPriceList(4, 100, 10);

            // When
            List<TechnicalIndicators> results = TechnicalIndicatorCalculator.calculateSeries(prices);

            // Then
            assertThat(results).hasSize(4);
            assertThat(results.get(3).getMa5())
                    .as("데이터가 5개 미만이면 MA5는 null이어야 함")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("2. RSI (상대강도지수) 검증")
    class RsiTest {

        @Test
        @DisplayName("주가가 지속 상승하면 RSI는 높은 값(100 근접)을 유지한다")
        void rsi_uptrend() {
            // Given: 30일간 매일 10원씩 상승
            List<BigDecimal> prices = createPriceList(30, 100, 10);

            // When
            List<TechnicalIndicators> results = TechnicalIndicatorCalculator.calculateSeries(prices);

            // Then
            assertThat(results.get(14).getRsi14())
                    .as("무조건 상승 시 RSI는 100이어야 함")
                    .isEqualByComparingTo(BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("변동이 없으면(Flat) 0 나누기 에러 없이 처리된다")
        void rsi_flat() {
            // Given: 30일간 가격 변동 없음 (100, 100, ...)
            List<BigDecimal> prices = createPriceList(30, 100, 0);

            // When
            List<TechnicalIndicators> results = TechnicalIndicatorCalculator.calculateSeries(prices);

            // Then
            assertThat(results.get(29).getRsi14())
                    .isNotNull()
                    .isEqualByComparingTo(BigDecimal.valueOf(0)); // ta4j returns 0 for flat
        }
    }

    @Nested
    @DisplayName("3. MACD 검증")
    class MacdTest {

        @Test
        @DisplayName("MACD는 EMA26이 계산된 이후부터 값이 존재한다")
        void macd_initialization() {
            // Given: 60일치 데이터
            List<BigDecimal> prices = createPriceList(60, 100, 10);

            // When
            List<TechnicalIndicators> results = TechnicalIndicatorCalculator.calculateSeries(prices);

            // Then
            assertThat(results.get(24).getMacd())
                    .as("25일차까지는 MACD 계산 불가 (EMA26 미달)")
                    .isNull();

            assertThat(results.get(34).getMacd())
                    .as("35일차부터 MACD 및 Signal 값 존재")
                    .isNotNull();
        }
    }

    @Nested
    @DisplayName("4. 데이터 무결성 및 예외 처리 (Critical)")
    class IntegrityTest {

        @Test
        @DisplayName("중간에 Null(거래정지)이 포함되어도 계산이 중단되지 않는다")
        void handle_null_values() {
            // Given: 10개 데이터 중 중간에 null 포함
            List<BigDecimal> prices = new ArrayList<>(Collections.nCopies(10, null));
            prices.set(0, BigDecimal.valueOf(100));
            prices.set(1, BigDecimal.valueOf(110));
            prices.set(2, null); 
            prices.set(3, BigDecimal.valueOf(120));
            prices.set(4, BigDecimal.valueOf(130));

            // When
            List<TechnicalIndicators> results = TechnicalIndicatorCalculator.calculateSeries(prices);

            // Then
            assertThat(results).hasSize(10);
            assertThat(results.get(4).getMa5())
                    .as("유효 데이터가 부족하므로 null 반환 (ma5는 5개 필요)")
                    .isNull();
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2})
        @DisplayName("극단적으로 적은 데이터(0~2개)가 들어와도 빈 리스트나 null을 안전하게 반환")
        void handle_small_size(int size) {
            List<BigDecimal> prices = createPriceList(size, 100, 10);

            List<TechnicalIndicators> results = TechnicalIndicatorCalculator.calculateSeries(prices);

            if (size == 0) {
                assertThat(results).isEmpty();
            } else {
                assertThat(results).hasSize(size);
                assertThat(results.get(0).getMa5()).isNull();
            }
        }
    }
}
