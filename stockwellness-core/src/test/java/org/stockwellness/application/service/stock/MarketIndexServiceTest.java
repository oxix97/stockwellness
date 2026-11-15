package org.stockwellness.application.service.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.stock.result.MarketDashboardResult;
import org.stockwellness.application.port.in.stock.result.MarketWeatherLevel;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.IndexDailyReadPort;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.domain.stock.exception.StockPriceException;
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
    private IndexDailyReadPort indexDailyReadPort;

    private MarketIndexService marketIndexService;

    @BeforeEach
    void setUp() {
        MarketWeatherFactory marketWeatherFactory = new MarketWeatherFactory();
        MarketWeatherClassifier marketWeatherClassifier = new MarketWeatherClassifier(marketWeatherFactory);
        marketIndexService = new MarketIndexService(
                loadBenchmarkPort,
                marketWeatherClassifier,
                indexDailyReadPort
        );
    }

    @Test
    @DisplayName("지수 변화율이 없으면 0으로 만들지 않고 FINAL 기상도가 없으면 날씨를 비운다")
    void getMarketIndexes_returnsMappedResult() {
        LocalDate baseDate = LocalDate.now().minusDays(1);

        // 벤치마크 데이터 모킹
        List<StockPriceResult> mockPrices = new ArrayList<>();
        for (BenchmarkType type : BenchmarkType.values()) {
            mockPrices.add(priceResult(baseDate, type == BenchmarkType.KOSPI ? null : "1.00", type.getTicker()));
        }

        given(loadBenchmarkPort.loadBenchmarkPricesIn(anyList(), any(), any())).willReturn(mockPrices);
        
        MarketDashboardResult dashboard = marketIndexService.getMarketIndexes();
        
        assertThat(dashboard.indexes()).hasSize(BenchmarkType.values().length);
        assertThat(dashboard.indexes().stream().filter(index -> index.ticker().equals("0001")).findFirst()
                .orElseThrow().fluctuationRate()).isNull();
        assertThat(dashboard.weather()).isNull();
    }

    @Test
    @DisplayName("FINAL KOSPI 기상 점수를 새 구조에서 읽어 분류한다")
    void getMarketIndexes_usesFinalWeatherScore() {
        LocalDate baseDate = LocalDate.now().minusDays(1);
        List<StockPriceResult> mockPrices = new ArrayList<>();
        for (BenchmarkType type : BenchmarkType.values()) {
            mockPrices.add(priceResult(baseDate, "1.00", type.getTicker()));
        }
        given(loadBenchmarkPort.loadBenchmarkPricesIn(anyList(), any(), any())).willReturn(mockPrices);
        given(indexDailyReadPort.finalWeatherScore("0001", baseDate)).willReturn(Optional.of(BigDecimal.valueOf(80)));

        MarketDashboardResult dashboard = marketIndexService.getMarketIndexes();

        assertThat(dashboard.weather().weatherLevel()).isEqualTo(MarketWeatherLevel.SUNNY);
    }

    @Test
    @DisplayName("지수 데이터가 없으면 예외를 던진다")
    void getMarketIndexes_throwsWhenPricesMissing() {
        given(loadBenchmarkPort.loadBenchmarkPricesIn(anyList(), any(), any())).willReturn(Collections.emptyList());

        assertThatThrownBy(() -> marketIndexService.getMarketIndexes())
                .isInstanceOf(StockPriceException.class);
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
                changeRate == null ? null : new BigDecimal(changeRate),
                ticker
        );
    }
}
