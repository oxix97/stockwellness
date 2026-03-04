package org.stockwellness.batch.job.stock.price;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.AlignmentStatus;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.stockwellness.batch.job.stock.price.StockPriceTestFixture.createSamsungStock;

class Samsung2023PriceSyncTest {

    private StockPriceProcessor processor;
    private KisDailyPriceAdapter kisAdapter;
    private StockPricePort stockPricePort;
    private RateLimiter kisRateLimiter;
    private ObjectMapper objectMapper;

    private Stock samsung;

    @BeforeEach
    void setUp() {
        kisAdapter = Mockito.mock(KisDailyPriceAdapter.class);
        stockPricePort = Mockito.mock(StockPricePort.class);
        kisRateLimiter = Mockito.mock(RateLimiter.class);
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        processor = new StockPriceProcessor(kisAdapter, stockPricePort, kisRateLimiter);
        samsung = createSamsungStock();

        when(kisRateLimiter.executeSupplier(any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });
    }

    @Test
    @DisplayName("삼성전자 2023년 실데이터 기반 지표 정밀 검증")
    void testSamsung2023FullSync() throws Exception {
        // given
        InputStream is = new ClassPathResource("samsung_2023_prices.json").getInputStream();
        JsonNode root = objectMapper.readTree(is);
        JsonNode output2 = root.get("output2");
        
        List<KisDailyPriceDetail> apiResults = new ArrayList<>();
        for (JsonNode node : output2) {
            apiResults.add(objectMapper.treeToValue(node, KisDailyPriceDetail.class));
        }
        
        ReflectionTestUtils.setField(processor, "startDateStr", "20230101");
        ReflectionTestUtils.setField(processor, "endDateStr", "20231231");

        when(stockPricePort.findLatestBaseDatesByStocks(any())).thenReturn(Collections.emptyMap());
        when(stockPricePort.findRecentPricesWithDateByStocks(any(), any(), anyInt())).thenReturn(Collections.emptyMap());
        when(kisAdapter.fetchDailyPrices(eq(samsung), any(), any())).thenReturn(apiResults);

        // when
        List<StockPrice> result = processor.process(List.of(samsung));

        // then
        assertThat(result).isNotEmpty();
        result.sort(Comparator.comparing(p -> p.getId().getBaseDate()));

        // 2023-12-28 데이터 검증
        LocalDate targetDate = LocalDate.of(2023, 12, 28);
        StockPrice target = result.stream()
                .filter(p -> p.getId().getBaseDate().equals(targetDate))
                .findFirst()
                .orElseThrow();

        TechnicalIndicators ti = target.getIndicators();

        // MA5 수동 계산 비교
        BigDecimal manualMa5 = BigDecimal.valueOf(78500 + 78000 + 76600 + 75900 + 75000)
                .divide(BigDecimal.valueOf(5), 4, RoundingMode.HALF_UP);
        assertThat(ti.getMa5()).isEqualByComparingTo(manualMa5);

        // 기본 구조 검증
        assertThat(ti.getBollingerUpper()).isGreaterThan(ti.getBollingerMid());
        assertThat(ti.getAlignmentStatus()).isNotNull();
    }
}
