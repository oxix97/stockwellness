package org.stockwellness.application.service.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.stock.result.MarketIndexResult;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
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

    @InjectMocks
    private MarketIndexService marketIndexService;

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

        List<MarketIndexResult> results = marketIndexService.getMarketIndexes();

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
}
