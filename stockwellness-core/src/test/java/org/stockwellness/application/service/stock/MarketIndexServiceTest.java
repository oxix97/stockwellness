package org.stockwellness.application.service.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.stock.result.MarketDashboardResult;
import org.stockwellness.application.port.in.stock.result.MarketIndexResult;
import org.stockwellness.application.port.in.stock.result.MarketWeatherLevel;
import org.stockwellness.application.port.in.stock.result.MarketWeatherReason;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockSector;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.domain.stock.exception.StockPriceException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MarketIndexServiceTest {

    @Mock
    private LoadBenchmarkPort loadBenchmarkPort;

    @Mock
    private StockPricePort stockPricePort;

    private MarketIndexService marketIndexService;

    @BeforeEach
    void setUp() {
        MarketWeatherFactory marketWeatherFactory = new MarketWeatherFactory();
        MarketWeatherClassifier marketWeatherClassifier = new MarketWeatherClassifier(marketWeatherFactory);
        MarketBreadthCalculator marketBreadthCalculator = new MarketBreadthCalculator();
        marketIndexService = new MarketIndexService(
                loadBenchmarkPort,
                stockPricePort,
                marketBreadthCalculator,
                marketWeatherClassifier
        );
    }

    @Test
    @DisplayName("최근 지수 데이터가 있으면 응답에 티커와 가격 정보를 담는다")
    void getMarketIndexes_returnsMappedResult() {
        LocalDate baseDate = LocalDate.now().minusDays(1);

        given(loadBenchmarkPort.loadBenchmarkPrices(anyString(), any(), any()))
                .willReturn(List.of(new StockPriceResult(
                        baseDate,
                        new BigDecimal("2500.00"),
                        new BigDecimal("2530.00"),
                        new BigDecimal("2490.00"),
                        new BigDecimal("2525.00"),
                        new BigDecimal("2525.00"),
                        1000L,
                        BigDecimal.ZERO,
                        null,
                        null,
                        null,
                        null,
                        new BigDecimal("1.00")
                )));
        given(stockPricePort.findLatestDateOnOrBefore(any())).willReturn(java.util.Optional.of(baseDate));
        given(stockPricePort.findAllByDate(baseDate)).willReturn(List.of(
                stockPrice(new BigDecimal("1.2")),
                stockPrice(new BigDecimal("0.8")),
                stockPrice(new BigDecimal("0.6")),
                stockPrice(new BigDecimal("-0.2"))
        ));

        MarketDashboardResult dashboard = marketIndexService.getMarketIndexes();
        List<MarketIndexResult> results = dashboard.indexes();

        MarketIndexResult kospi = results.stream()
                .filter(result -> result.ticker().equals(BenchmarkType.KOSPI.getTicker()))
                .findFirst()
                .orElseThrow();

        assertThat(results).hasSize(BenchmarkType.values().length);
        assertThat(kospi.name()).isEqualTo("코스피 종합");
        assertThat(kospi.currentPrice()).isEqualByComparingTo("2525.00");
        assertThat(kospi.fluctuationRate()).isEqualByComparingTo("1.00");
        assertThat(kospi.fluctuationAmount()).isEqualByComparingTo("25.0000");
        assertThat(kospi.history()).hasSize(1);
        assertThat(kospi.history().get(0).date()).isEqualTo(baseDate);
        assertThat(dashboard.weather().weatherLevel()).isEqualTo(MarketWeatherLevel.SUNNY);
        assertThat(dashboard.weather().reasonCode()).isEqualTo(MarketWeatherReason.STEADY_ADVANCE);
        assertThat(dashboard.weather().asOfDate()).isEqualTo(baseDate);
    }

    @Test
    @DisplayName("전체 지수 데이터가 비면 0 응답 대신 예외를 던진다")
    void getMarketIndexes_throwsWhenAllBenchmarksAreMissing() {
        given(loadBenchmarkPort.loadBenchmarkPrices(anyString(), any(), any()))
                .willReturn(Collections.emptyList());

        assertThatThrownBy(() -> marketIndexService.getMarketIndexes())
                .isInstanceOf(StockPriceException.class)
                .hasMessageContaining("해당 기간의 시세 데이터를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("일부 지수 데이터만 비어도 0 fallback 대신 예외를 던진다")
    void getMarketIndexes_throwsWhenAnyBenchmarkIsMissing() {
        LocalDate baseDate = LocalDate.now().minusDays(1);
        given(loadBenchmarkPort.loadBenchmarkPrices(anyString(), any(), any()))
                .willReturn(List.of(new StockPriceResult(
                        baseDate,
                        new BigDecimal("2500.00"),
                        new BigDecimal("2530.00"),
                        new BigDecimal("2490.00"),
                        new BigDecimal("2525.00"),
                        new BigDecimal("2525.00"),
                        1000L,
                        BigDecimal.ZERO,
                        null,
                        null,
                        null,
                        null,
                        new BigDecimal("1.00")
                )));
        given(loadBenchmarkPort.loadBenchmarkPrices(org.mockito.ArgumentMatchers.eq(BenchmarkType.S_P_500.getTicker()), any(), any()))
                .willReturn(Collections.emptyList());

        assertThatThrownBy(() -> marketIndexService.getMarketIndexes())
                .isInstanceOf(StockPriceException.class)
                .hasMessageContaining("해당 기간의 시세 데이터를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("지수는 버티지만 하락 종목이 우세하면 안개 단계로 분류한다")
    void getMarketIndexes_classifiesFoggyWhenBreadthIsWeak() {
        LocalDate baseDate = LocalDate.now().minusDays(1);
        given(loadBenchmarkPort.loadBenchmarkPrices(anyString(), any(), any()))
                .willReturn(List.of(priceResult(baseDate, "0.10")));
        given(loadBenchmarkPort.loadBenchmarkPrices(org.mockito.ArgumentMatchers.eq(BenchmarkType.KOSPI.getTicker()), any(), any()))
                .willReturn(List.of(priceResult(baseDate, "0.15")));
        given(loadBenchmarkPort.loadBenchmarkPrices(org.mockito.ArgumentMatchers.eq(BenchmarkType.KOSDAQ.getTicker()), any(), any()))
                .willReturn(List.of(priceResult(baseDate, "-0.65")));
        given(stockPricePort.findLatestDateOnOrBefore(any())).willReturn(java.util.Optional.of(baseDate));
        given(stockPricePort.findAllByDate(baseDate)).willReturn(List.of(
                stockPrice(new BigDecimal("-1.8")),
                stockPrice(new BigDecimal("-0.9")),
                stockPrice(new BigDecimal("-0.6")),
                stockPrice(new BigDecimal("0.2")),
                stockPrice(new BigDecimal("0.1"))
        ));

        MarketDashboardResult dashboard = marketIndexService.getMarketIndexes();

        assertThat(dashboard.weather().weatherLevel()).isEqualTo(MarketWeatherLevel.FOGGY);
        assertThat(dashboard.weather().reasonCode()).isEqualTo(MarketWeatherReason.HIDDEN_WEAKNESS);
    }

    @Test
    @DisplayName("급락과 변동성 확대가 함께 오면 폭우 단계로 분류한다")
    void getMarketIndexes_classifiesStormyWhenSellOffIsVolatile() {
        LocalDate baseDate = LocalDate.now().minusDays(1);
        given(loadBenchmarkPort.loadBenchmarkPrices(anyString(), any(), any()))
                .willReturn(List.of(priceResult(baseDate, "-1.20")));
        given(loadBenchmarkPort.loadBenchmarkPrices(org.mockito.ArgumentMatchers.eq(BenchmarkType.KOSPI.getTicker()), any(), any()))
                .willReturn(List.of(priceResult(baseDate, "-1.80")));
        given(loadBenchmarkPort.loadBenchmarkPrices(org.mockito.ArgumentMatchers.eq(BenchmarkType.KOSDAQ.getTicker()), any(), any()))
                .willReturn(List.of(priceResult(baseDate, "-2.40")));
        given(stockPricePort.findLatestDateOnOrBefore(any())).willReturn(java.util.Optional.of(baseDate));
        given(stockPricePort.findAllByDate(baseDate)).willReturn(List.of(
                volatileStockPrice("-4.5", "8.0"),
                volatileStockPrice("-3.7", "6.5"),
                volatileStockPrice("-3.2", "5.1"),
                volatileStockPrice("-2.9", "4.8"),
                stockPrice(new BigDecimal("-0.4"))
        ));

        MarketDashboardResult dashboard = marketIndexService.getMarketIndexes();

        assertThat(dashboard.weather().weatherLevel()).isEqualTo(MarketWeatherLevel.STORMY);
        assertThat(dashboard.weather().reasonCode()).isEqualTo(MarketWeatherReason.VOLATILE_SELL_OFF);
    }

    private StockPriceResult priceResult(LocalDate baseDate, String changeRate) {
        return new StockPriceResult(
                baseDate,
                new BigDecimal("2500.00"),
                new BigDecimal("2530.00"),
                new BigDecimal("2490.00"),
                new BigDecimal("2525.00"),
                new BigDecimal("2525.00"),
                1000L,
                BigDecimal.ZERO,
                null,
                null,
                null,
                null,
                new BigDecimal(changeRate)
        );
    }

    private StockPrice stockPrice(BigDecimal rate) {
        return volatileStockPrice(rate.toPlainString(), "1.5");
    }

    private StockPrice volatileStockPrice(String rateText, String intradaySwingPercent) {
        BigDecimal previousClose = new BigDecimal("100");
        BigDecimal close = previousClose.multiply(BigDecimal.ONE.add(new BigDecimal(rateText).divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)));
        BigDecimal high = previousClose.multiply(BigDecimal.ONE.add(new BigDecimal(intradaySwingPercent).divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)));
        BigDecimal low = previousClose;

        return StockPrice.of(
                Stock.of("TEST" + rateText.replace("-", "N").replace(".", ""), null, "테스트", MarketType.KOSPI, Currency.KRW, StockSector.empty(), StockStatus.ACTIVE),
                LocalDate.now(),
                previousClose,
                high,
                low,
                close,
                close,
                previousClose,
                1000L,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                TechnicalIndicators.empty()
        );
    }
}
