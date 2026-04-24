package org.stockwellness.application.service.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.stock.result.MarketWeatherLevel;
import org.stockwellness.application.port.in.stock.result.MarketWeatherReason;
import org.stockwellness.application.port.in.stock.result.MarketWeatherResult;
import org.stockwellness.application.port.out.stock.MarketBreadthSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketWeatherClassifier 단위 테스트")
class MarketWeatherClassifierTest {

    @Mock
    private MarketWeatherFactory marketWeatherFactory;

    @InjectMocks
    private MarketWeatherClassifier classifier;

    @Test
    @DisplayName("의존성 주입 확인")
    void setup() {
        assertThat(classifier).isNotNull();
    }

    @Test
    @DisplayName("코스피 급락 및 변동성 확대 시 STORMY를 반환한다")
    void classify_stormy() {
        // Given
        BigDecimal kospiRate = new BigDecimal("-2.0");
        BigDecimal kosdaqRate = new BigDecimal("-0.5");
        MarketBreadthSnapshot breadth = new MarketBreadthSnapshot(
                370, 100, 200, 50, 20,
                new BigDecimal("0.3"), // advance
                new BigDecimal("0.6"), // decline
                new BigDecimal("0.3")  // high volatility (> 0.22)
        );
        LocalDate date = LocalDate.of(2026, 4, 24);
        MarketWeatherResult expected = new MarketWeatherResult(
                MarketWeatherLevel.STORMY, "Message", "Description", MarketWeatherReason.VOLATILE_SELL_OFF, date);

        given(marketWeatherFactory.create(eq(MarketWeatherLevel.STORMY), eq(MarketWeatherReason.VOLATILE_SELL_OFF), eq(date)))
                .willReturn(expected);

        // When
        MarketWeatherResult result = classifier.classify(kospiRate, kosdaqRate, breadth, date);

        // Then
        assertThat(result).isEqualTo(expected);
        verify(marketWeatherFactory).create(MarketWeatherLevel.STORMY, MarketWeatherReason.VOLATILE_SELL_OFF, date);
    }

    @Test
    @DisplayName("지수 상승 및 상승 종목 확산 시 CLEAR를 반환한다")
    void classify_clear() {
        // Given
        BigDecimal kospiRate = new BigDecimal("1.5");
        BigDecimal kosdaqRate = new BigDecimal("1.0");
        MarketBreadthSnapshot breadth = new MarketBreadthSnapshot(
                330, 200, 100, 20, 10,
                new BigDecimal("0.7"), // advance (> 0.62)
                new BigDecimal("0.2"), // decline
                new BigDecimal("0.1")  // high volatility (< 0.28)
        );
        LocalDate date = LocalDate.of(2026, 4, 24);
        MarketWeatherResult expected = new MarketWeatherResult(
                MarketWeatherLevel.CLEAR, "Message", "Description", MarketWeatherReason.BROAD_RALLY, date);

        given(marketWeatherFactory.create(eq(MarketWeatherLevel.CLEAR), eq(MarketWeatherReason.BROAD_RALLY), eq(date)))
                .willReturn(expected);

        // When
        MarketWeatherResult result = classifier.classify(kospiRate, kosdaqRate, breadth, date);

        // Then
        assertThat(result).isEqualTo(expected);
        verify(marketWeatherFactory).create(MarketWeatherLevel.CLEAR, MarketWeatherReason.BROAD_RALLY, date);
    }

    @Test
    @DisplayName("상승 종목 데이터(Breadth)가 없을 때 지수만으로 CLEAR를 판별한다")
    void classify_index_only() {
        // Given
        BigDecimal kospiRate = new BigDecimal("1.2");
        BigDecimal kosdaqRate = new BigDecimal("1.0");
        LocalDate date = LocalDate.of(2026, 4, 24);
        MarketWeatherResult expected = new MarketWeatherResult(
                MarketWeatherLevel.CLEAR, "Message", "Description", MarketWeatherReason.INDEX_ONLY_RALLY, date);

        given(marketWeatherFactory.create(eq(MarketWeatherLevel.CLEAR), eq(MarketWeatherReason.INDEX_ONLY_RALLY), eq(date)))
                .willReturn(expected);

        // When
        MarketWeatherResult result = classifier.classify(kospiRate, kosdaqRate, null, date);

        // Then
        assertThat(result).isEqualTo(expected);
        verify(marketWeatherFactory).create(MarketWeatherLevel.CLEAR, MarketWeatherReason.INDEX_ONLY_RALLY, date);
    }
}
