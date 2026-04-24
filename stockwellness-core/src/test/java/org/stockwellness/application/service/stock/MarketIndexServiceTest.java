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
import org.stockwellness.application.port.in.stock.result.MarketWeatherResult;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.application.port.out.stock.MarketBreadthSnapshot;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.domain.stock.exception.StockPriceException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
        marketIndexService = new MarketIndexService(
                loadBenchmarkPort,
                stockPricePort,
                null, // MarketBreadthCalculator는 이제 서비스에서 직접 사용하지 않음
                marketWeatherClassifier
        );
    }

    @Test
    @DisplayName("최근 지수 데이터가 있으면 응답에 티커와 가격 정보를 담는다")
    void getMarketIndexes_returnsMappedResult() {
        LocalDate baseDate = LocalDate.now().minusDays(1);

        // 9개 지수 데이터 모킹
        List<StockPriceResult> mockPrices = new ArrayList<>();
        for (BenchmarkType type : BenchmarkType.values()) {
            mockPrices.add(priceResult(baseDate, "1.00", type.getTicker()));
        }

        given(loadBenchmarkPort.loadBenchmarkPricesIn(anyList(), any(), any())).willReturn(mockPrices);
        given(stockPricePort.findLatestDateOnOrBefore(any())).willReturn(java.util.Optional.of(baseDate));
        given(stockPricePort.summarizeBreadthByDate(baseDate)).willReturn(new MarketBreadthSnapshot(
                100, 60, 20, 20, 10,
                new BigDecimal("0.60"), new BigDecimal("0.20"), new BigDecimal("0.10")
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
        assertThat(kospi.history()).hasSize(1);
        assertThat(dashboard.weather().weatherLevel()).isEqualTo(MarketWeatherLevel.SUNNY);
        assertThat(dashboard.weather().asOfDate()).isEqualTo(baseDate);
    }

    @Test
    @DisplayName("전체 지수 데이터가 비면 예외를 던진다")
    void getMarketIndexes_throwsWhenAllBenchmarksAreMissing() {
        given(loadBenchmarkPort.loadBenchmarkPricesIn(anyList(), any(), any()))
                .willReturn(Collections.emptyList());

        assertThatThrownBy(() -> marketIndexService.getMarketIndexes())
                .isInstanceOf(StockPriceException.class)
                .hasMessageContaining("해당 기간의 시세 데이터를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("일부 지수 데이터만 비어도 예외를 던진다")
    void getMarketIndexes_throwsWhenAnyBenchmarkIsMissing() {
        LocalDate baseDate = LocalDate.now().minusDays(1);
        List<StockPriceResult> partialPrices = List.of(priceResult(baseDate, "1.00", BenchmarkType.KOSPI.getTicker()));
        
        given(loadBenchmarkPort.loadBenchmarkPricesIn(anyList(), any(), any()))
                .willReturn(partialPrices);

        assertThatThrownBy(() -> marketIndexService.getMarketIndexes())
                .isInstanceOf(StockPriceException.class)
                .hasMessageContaining("해당 기간의 시세 데이터를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("지수는 버티지만 하락 종목이 우세하면 안개 단계로 분류한다")
    void getMarketIndexes_classifiesFoggyWhenBreadthIsWeak() {
        LocalDate baseDate = LocalDate.now().minusDays(1);
        List<StockPriceResult> mockPrices = new ArrayList<>();
        for (BenchmarkType type : BenchmarkType.values()) {
            String rate = "0.00";
            if (type == BenchmarkType.KOSPI) rate = "0.15";
            if (type == BenchmarkType.KOSDAQ) rate = "-0.65";
            mockPrices.add(priceResult(baseDate, rate, type.getTicker()));
        }

        given(loadBenchmarkPort.loadBenchmarkPricesIn(anyList(), any(), any())).willReturn(mockPrices);
        given(stockPricePort.findLatestDateOnOrBefore(any())).willReturn(java.util.Optional.of(baseDate));
        // 안개 요건: 하락 종목 비율이 높음
        given(stockPricePort.summarizeBreadthByDate(baseDate)).willReturn(new MarketBreadthSnapshot(
                100, 20, 60, 20, 5,
                new BigDecimal("0.20"), new BigDecimal("0.60"), new BigDecimal("0.05")
        ));

        MarketDashboardResult dashboard = marketIndexService.getMarketIndexes();

        assertThat(dashboard.weather().weatherLevel()).isEqualTo(MarketWeatherLevel.FOGGY);
        assertThat(dashboard.weather().reasonCode()).isEqualTo(MarketWeatherReason.HIDDEN_WEAKNESS);
    }

    private StockPriceResult priceResult(LocalDate baseDate, String changeRate, String ticker) {
        return new StockPriceResult(
                baseDate,
                new BigDecimal("2500.00"),
                new BigDecimal("2530.00"),
                new BigDecimal("2490.00"),
                new BigDecimal("2525.00"),
                new BigDecimal("2525.00"),
                1000L,
                BigDecimal.ZERO,
                null, null, null, null,
                new BigDecimal(changeRate),
                ticker
        );
    }
}
