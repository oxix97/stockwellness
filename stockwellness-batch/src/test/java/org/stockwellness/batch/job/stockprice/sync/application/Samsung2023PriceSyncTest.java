package org.stockwellness.batch.job.stockprice.sync.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.stockwellness.application.port.in.batch.StockPriceSyncUseCase;
import org.stockwellness.application.port.out.stock.DailyStockPriceSnapshot;
import org.stockwellness.application.port.out.stock.InvestorTradingSnapshot;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.batch.job.stockprice.sync.application.StockPriceBatchService;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.stockwellness.batch.job.stockprice.sync.support.StockPriceTestFixture.createSamsungStock;

class Samsung2023PriceSyncTest {

    private StockPriceBatchService stockPriceBatchService;
    private StockPricePort stockPricePort;
    private ObjectMapper objectMapper;

    private Stock samsung;

    @BeforeEach
    void setUp() {
        stockPricePort = Mockito.mock(StockPricePort.class);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        stockPriceBatchService = new StockPriceBatchService(stockPricePort);
        samsung = createSamsungStock();
    }

    @Test
    @DisplayName("삼성전자 2023년 실데이터 기반 지표 정밀 검증")
    void testSamsung2023FullSync() throws Exception {
        InputStream is = new ClassPathResource("samsung_2023_prices.json").getInputStream();
        JsonNode root = objectMapper.readTree(is);
        JsonNode output2 = root.get("output2");

        List<DailyStockPriceSnapshot> apiResults = new ArrayList<>();
        for (JsonNode node : output2) {
            var detail = objectMapper.treeToValue(node, org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail.class);
            apiResults.add(new DailyStockPriceSnapshot(
                    detail.baseDate(),
                    detail.openPrice(),
                    detail.highPrice(),
                    detail.lowPrice(),
                    detail.closePrice(),
                    detail.volume(),
                    detail.transactionAmt()
            ));
        }

        when(stockPricePort.findLatestBaseDatesByStocks(any())).thenReturn(Collections.emptyMap());
        when(stockPricePort.fetchDailyPrices(eq(samsung), any(), any())).thenReturn(apiResults);
        when(stockPricePort.fetchInvestorTradingSnapshots(eq(samsung), any(), any())).thenReturn(List.<InvestorTradingSnapshot>of());

        List<StockPrice> mockDbPrices = apiResults.stream()
                .map(snapshot -> StockPrice.of(
                        samsung, snapshot.baseDate(),
                        snapshot.openPrice(), snapshot.highPrice(), snapshot.lowPrice(), snapshot.closePrice(),
                        snapshot.closePrice(), BigDecimal.ZERO,
                        snapshot.volume(), snapshot.transactionAmt(),
                        TechnicalIndicators.empty()
                )).toList();
        when(stockPricePort.findRecentPricesWithDateByStocks(any(), any(), anyInt())).thenReturn(Map.of(samsung.getId(), mockDbPrices));

        List<StockPrice> result = new ArrayList<>(stockPriceBatchService.sync(
                new StockPriceSyncUseCase.StockPriceBatchCommand(List.of(samsung), "20230101", "20231231")
        ).stockPrices());

        assertThat(result).isNotEmpty();
        result.sort(Comparator.comparing(p -> p.getId().getBaseDate()));

        LocalDate targetDate = LocalDate.of(2023, 12, 28);
        StockPrice target = result.stream()
                .filter(p -> p.getId().getBaseDate().equals(targetDate))
                .findFirst()
                .orElseThrow();

        TechnicalIndicators ti = target.getIndicators();
        BigDecimal manualMa5 = BigDecimal.valueOf(78500 + 78000 + 76600 + 75900 + 75000)
                .divide(BigDecimal.valueOf(5), 4, RoundingMode.HALF_UP);
        assertThat(ti.getMa5()).isEqualByComparingTo(manualMa5);
        assertThat(ti.getBollingerUpper()).isGreaterThan(ti.getBollingerMid());
        assertThat(ti.getAlignmentStatus()).isNotNull();
    }
}
