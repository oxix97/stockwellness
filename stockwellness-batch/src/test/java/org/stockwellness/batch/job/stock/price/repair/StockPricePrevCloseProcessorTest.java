package org.stockwellness.batch.job.stock.price.repair;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.StockPriceId;
import org.stockwellness.domain.stock.price.TechnicalIndicators;
import org.stockwellness.fixture.StockFixture;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StockPricePrevCloseProcessorTest {

    private StockPricePrevCloseProcessor processor;
    private StockPriceRepository stockPriceRepository;

    @BeforeEach
    void setUp() {
        stockPriceRepository = mock(StockPriceRepository.class);
        processor = new StockPricePrevCloseProcessor(stockPriceRepository);
    }

    @Test
    @DisplayName("전일 종가 보정 테스트: 지정된 날짜 범위 내의 누락된 데이터를 정확히 찾아낸다")
    void process_repair_in_range() {
        // given
        Stock stock = StockFixture.createSamsung();
        ReflectionTestUtils.setField(stock, "id", 1L);

        LocalDate d1 = LocalDate.of(2022, 1, 1);
        LocalDate d2 = LocalDate.of(2022, 1, 2);
        LocalDate d3 = LocalDate.of(2022, 1, 3);

        // 시세 데이터 준비 (d2, d3의 전일 종가가 누락됨)
        StockPrice p1 = createPrice(stock, d1, "100", null);
        StockPrice p2 = createPrice(stock, d2, "110", null);
        StockPrice p3 = createPrice(stock, d3, "120", BigDecimal.ZERO);

        when(stockPriceRepository.findByStockIdOrderByBaseDateAsc(1L))
                .thenReturn(List.of(p1, p2, p3));

        // 파라미터 설정 (2022-01-02 ~ 2022-01-03 범위만 보정)
        ReflectionTestUtils.setField(processor, "startDateStr", "20220102");
        ReflectionTestUtils.setField(processor, "endDateStr", "20220103");

        // when
        List<StockPriceRepairDto> result = processor.process(stock);

        // then
        assertThat(result).hasSize(2);
        
        // d2 보정 확인: 전날(d1)의 종가 100이 들어가야 함
        assertThat(result.get(0).baseDate()).isEqualTo(d2);
        assertThat(result.get(0).calculatedPrevClose()).isEqualByComparingTo("100");

        // d3 보정 확인: 전날(d2)의 종가 110이 들어가야 함
        assertThat(result.get(1).baseDate()).isEqualTo(d3);
        assertThat(result.get(1).calculatedPrevClose()).isEqualByComparingTo("110");
    }

    @Test
    @DisplayName("날짜 범위를 벗어난 데이터는 보정 대상에서 제외된다")
    void process_out_of_range_ignored() {
        // given
        Stock stock = StockFixture.createSamsung();
        ReflectionTestUtils.setField(stock, "id", 1L);

        LocalDate d1 = LocalDate.of(2022, 1, 1);
        LocalDate d2 = LocalDate.of(2022, 1, 2);

        StockPrice p1 = createPrice(stock, d1, "100", null);
        StockPrice p2 = createPrice(stock, d2, "110", null);

        when(stockPriceRepository.findByStockIdOrderByBaseDateAsc(1L))
                .thenReturn(List.of(p1, p2));

        // 파라미터 설정 (2022-01-01만 타겟팅 - p2는 범위 밖)
        ReflectionTestUtils.setField(processor, "startDateStr", "20220101");
        ReflectionTestUtils.setField(processor, "endDateStr", "20220101");

        // when
        List<StockPriceRepairDto> result = processor.process(stock);

        // then
        // p1은 전날 데이터가 없어서 스킵, p2는 범위 밖이라 스킵 -> 결과 null
        assertThat(result).isNull();
    }

    private StockPrice createPrice(Stock stock, LocalDate date, String close, BigDecimal prevClose) {
        return StockPrice.of(
                stock, date,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal(close),
                BigDecimal.ZERO, prevClose,
                0L, BigDecimal.ZERO,
                TechnicalIndicators.empty()
        );
    }
}
