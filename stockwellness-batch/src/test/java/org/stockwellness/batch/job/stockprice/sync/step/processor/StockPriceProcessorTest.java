package org.stockwellness.batch.job.stockprice.sync.step.processor;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.stockwellness.application.port.in.batch.StockPriceSyncUseCase;
import org.stockwellness.application.port.out.stock.DailyStockPriceSnapshot;
import org.stockwellness.application.port.out.stock.InvestorTradingSnapshot;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.batch.job.stockprice.sync.application.StockPriceBatchService;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;
import org.stockwellness.domain.stock.price.AlignmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.stockwellness.batch.job.stockprice.sync.support.StockPriceTestFixture.createSamsungStock;

class StockPriceProcessorTest {

    private StockPricePort stockPricePort;
    private StockPriceBatchService stockPriceBatchService;

    private Stock samsung;

    @BeforeEach
    void setUp() {
        stockPricePort = Mockito.mock(StockPricePort.class);
        stockPriceBatchService = new StockPriceBatchService(stockPricePort);
        samsung = createSamsungStock();
    }

    @Test
    @DisplayName("2022-01-01 이전 데이터 필터링 및 지표 계산 정밀도 통합 검증")
    void testSyncLogicAndFiltering() {
        LocalDate date2021 = LocalDate.of(2021, 12, 31);
        LocalDate date2022_1 = LocalDate.of(2022, 1, 1);
        LocalDate date2022_2 = LocalDate.of(2022, 1, 2);

        List<DailyStockPriceSnapshot> apiResults = List.of(
                dailySnapshot(date2022_2, 72000),
                dailySnapshot(date2022_1, 71000),
                dailySnapshot(date2021, 70000)
        );

        when(stockPricePort.findLatestBaseDatesByStocks(any())).thenReturn(Map.of());
        when(stockPricePort.findRecentPricesWithDateByStocks(any(), any(), anyInt())).thenReturn(Collections.emptyMap());
        when(stockPricePort.fetchDailyPrices(eq(samsung), any(), any())).thenAnswer(invocation -> {
            LocalDate start = invocation.getArgument(1);
            LocalDate end = invocation.getArgument(2);
            if (!start.isAfter(date2021) && !end.isBefore(date2022_2)) {
                return apiResults;
            }
            return List.of();
        });
        when(stockPricePort.fetchInvestorTradingSnapshots(eq(samsung), any(), any())).thenReturn(List.<InvestorTradingSnapshot>of());

        List<StockPrice> result = stockPriceBatchService.sync(
                new StockPriceSyncUseCase.StockPriceBatchCommand(List.of(samsung), "20211230", "20220102")
        ).stockPrices();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(p -> p.getId().getBaseDate()).doesNotContain(date2021);

        StockPrice jan1st = result.stream()
                .filter(price -> price.getId().getBaseDate().equals(date2022_1))
                .findFirst()
                .orElseThrow();
        assertThat(jan1st.getPreviousClosePrice()).isEqualByComparingTo("70000");
    }

    @Test
    @DisplayName("데이터 부족(신규 상장주) 시 기술적 지표 안전 처리 검증")
    void testInsufficientDataHandling() {
        List<DailyStockPriceSnapshot> apiResults = List.of(
                dailySnapshot(LocalDate.of(2024, 1, 3), 12000),
                dailySnapshot(LocalDate.of(2024, 1, 2), 11000),
                dailySnapshot(LocalDate.of(2024, 1, 1), 10000)
        );

        when(stockPricePort.findLatestBaseDatesByStocks(any())).thenReturn(Collections.emptyMap());
        when(stockPricePort.findRecentPricesWithDateByStocks(any(), any(), anyInt())).thenReturn(Collections.emptyMap());
        when(stockPricePort.fetchDailyPrices(eq(samsung), any(), any())).thenAnswer(invocation -> {
            LocalDate start = invocation.getArgument(1);
            LocalDate end = invocation.getArgument(2);
            if (!start.isAfter(LocalDate.of(2024, 1, 1)) && !end.isBefore(LocalDate.of(2024, 1, 3))) {
                return apiResults;
            }
            return List.of();
        });
        when(stockPricePort.fetchInvestorTradingSnapshots(eq(samsung), any(), any())).thenReturn(List.<InvestorTradingSnapshot>of());

        List<StockPrice> result = stockPriceBatchService.sync(
                new StockPriceSyncUseCase.StockPriceBatchCommand(List.of(samsung), "20240101", "20240103")
        ).stockPrices();

        assertThat(result).hasSize(3);
        TechnicalIndicators indicators = result.get(2).getIndicators();
        assertThat(indicators.getMa5()).isNull();
        assertThat(indicators.getAlignmentStatus()).isEqualTo(AlignmentStatus.MIXED);
    }



    private DailyStockPriceSnapshot dailySnapshot(LocalDate date, int price) {
        return new DailyStockPriceSnapshot(
                date,
                BigDecimal.valueOf(price),
                BigDecimal.valueOf(price + 100),
                BigDecimal.valueOf(price - 100),
                BigDecimal.valueOf(price),
                100000L,
                BigDecimal.valueOf(1_000_000_000L)
        );
    }
}
