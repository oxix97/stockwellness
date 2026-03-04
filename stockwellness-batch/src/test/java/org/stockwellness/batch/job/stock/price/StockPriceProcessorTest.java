package org.stockwellness.batch.job.stock.price;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.test.util.ReflectionTestUtils;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.stockwellness.batch.job.stock.price.StockPriceTestFixture.createDetail;
import static org.stockwellness.batch.job.stock.price.StockPriceTestFixture.createSamsungStock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockPriceProcessorTest {

    @Mock
    private KisDailyPriceAdapter kisAdapter;

    @Mock
    private StockPricePort stockPricePort;

    @Mock
    private RateLimiter kisRateLimiter;

    @InjectMocks
    private StockPriceProcessor processor;

    private Stock samsung;

    @BeforeEach
    void setUp() {
        samsung = createSamsungStock();
        
        when(kisRateLimiter.executeSupplier(any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });
    }

    @Test
    @DisplayName("2022-01-01 이전 데이터 필터링 및 지표 계산 정밀도 통합 검증")
    void testSyncLogicAndFiltering() throws Exception {
        // given
        ReflectionTestUtils.setField(processor, "startDateStr", "20211230");
        ReflectionTestUtils.setField(processor, "endDateStr", "20220102");

        LocalDate date2021 = LocalDate.of(2021, 12, 31);
        LocalDate date2022_1 = LocalDate.of(2022, 1, 1);
        LocalDate date2022_2 = LocalDate.of(2022, 1, 2);

        List<KisDailyPriceDetail> apiResults = List.of(
                createDetail(date2021, 70000),
                createDetail(date2022_1, 71000),
                createDetail(date2022_2, 72000)
        );

        when(stockPricePort.findLatestBaseDatesByStocks(any())).thenReturn(Map.of(samsung.getId(), date2021));
        when(stockPricePort.findRecentPricesWithDateByStocks(any(), any(), anyInt())).thenReturn(Collections.emptyMap());
        when(kisAdapter.fetchDailyPrices(eq(samsung), any(), any())).thenReturn(apiResults);

        // when
        List<StockPrice> result = processor.process(List.of(samsung));

        // then
        // 1. 하한선 필터링 검증: 2021년 데이터 제외
        assertThat(result).hasSize(2);
        assertThat(result).extracting(p -> p.getId().getBaseDate()).doesNotContain(date2021);

        // 2. 지표 연속성 검증: 첫 번째 저장일(1/1)의 전일 종가가 2021-12-31 가격이어야 함
        StockPrice jan1st = result.get(0);
        assertThat(jan1st.getPreviousClosePrice()).isEqualByComparingTo("70000");
    }

    @Test
    @DisplayName("데이터 부족(신규 상장주) 시 기술적 지표 안전 처리 검증")
    void testInsufficientDataHandling() throws Exception {
        // given: 데이터 3개만 존재
        List<KisDailyPriceDetail> apiResults = List.of(
                createDetail(LocalDate.of(2024, 1, 1), 10000),
                createDetail(LocalDate.of(2024, 1, 2), 11000),
                createDetail(LocalDate.of(2024, 1, 3), 12000)
        );

        ReflectionTestUtils.setField(processor, "startDateStr", "20240101");
        
        when(stockPricePort.findLatestBaseDatesByStocks(any())).thenReturn(Collections.emptyMap());
        when(stockPricePort.findRecentPricesWithDateByStocks(any(), any(), anyInt())).thenReturn(Collections.emptyMap());
        when(kisAdapter.fetchDailyPrices(eq(samsung), any(), any())).thenReturn(apiResults);

        // when
        List<StockPrice> result = processor.process(List.of(samsung));

        // then
        TechnicalIndicators ti = result.get(2).getIndicators();
        assertThat(ti.getMa5()).isNull(); // 5개 미만이므로 null
    }
}
