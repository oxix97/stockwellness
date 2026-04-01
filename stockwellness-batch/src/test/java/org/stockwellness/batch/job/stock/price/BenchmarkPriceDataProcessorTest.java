package org.stockwellness.batch.job.stock.price;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.adapter.out.external.kis.dto.BenchmarkPriceData;
import org.stockwellness.application.port.out.stock.BenchmarkPricePort;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.domain.stock.price.BenchmarkPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("BenchmarkPriceDataProcessor 단위 테스트")
class BenchmarkPriceDataProcessorTest {

    @InjectMocks
    private BenchmarkPriceDataProcessor processor;

    @Mock
    private BenchmarkPricePort benchmarkPricePort;

    @Test
    @DisplayName("국내 지수(KOSPI)의 API 등락률이 0인 경우 전일 종가를 기반으로 수동 계산한다")
    void process_domesticIndexWithZeroChangeRate_calculatesManually() {
        // given
        BenchmarkType type = BenchmarkType.KOSPI;
        LocalDate today = LocalDate.of(2026, 4, 2);
        
        // 오늘 KOSPI 데이터: 등락률이 0으로 들어옴
        BenchmarkPriceData data = mock(BenchmarkPriceData.class);
        given(data.baseDate()).willReturn(today);
        given(data.openPrice()).willReturn(BigDecimal.valueOf(2500));
        given(data.highPrice()).willReturn(BigDecimal.valueOf(2550));
        given(data.lowPrice()).willReturn(BigDecimal.valueOf(2480));
        given(data.closePrice()).willReturn(BigDecimal.valueOf(2525));
        given(data.prdyCtrt()).willReturn(BigDecimal.ZERO); // API에서 제공한 등락률 (오류 상황)
        given(data.volume()).willReturn(10000L);

        BenchmarkPriceDataWrapper wrapper = new BenchmarkPriceDataWrapper(type, data);

        // 어제 KOSPI 종가 데이터 모킹 (2500원)
        // 등락률 계산: (2525 - 2500) / 2500 * 100 = 1.00%
        BenchmarkPrice prevPrice = mock(BenchmarkPrice.class);
        given(prevPrice.getClosePrice()).willReturn(BigDecimal.valueOf(2500));
        given(benchmarkPricePort.findLatestBefore(eq(type.getTicker()), eq(today)))
                .willReturn(Optional.of(prevPrice));

        // when
        BenchmarkPrice result = processor.process(wrapper);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getChangeRate()).isEqualByComparingTo("1.00");
        assertThat(result.getClosePrice()).isEqualByComparingTo("2525");
    }

    @Test
    @DisplayName("국내 지수의 API 등락률이 정상적인 경우 수동 계산을 건너뛰고 API 값을 신뢰한다")
    void process_domesticIndexWithValidChangeRate_usesApiValue() {
        // given
        BenchmarkType type = BenchmarkType.KOSPI;
        LocalDate today = LocalDate.of(2026, 4, 2);
        
        // 정상적인 등락률(1.5%)이 포함된 데이터
        BenchmarkPriceData data = mock(BenchmarkPriceData.class);
        given(data.baseDate()).willReturn(today);
        given(data.openPrice()).willReturn(BigDecimal.valueOf(2500));
        given(data.highPrice()).willReturn(BigDecimal.valueOf(2550));
        given(data.lowPrice()).willReturn(BigDecimal.valueOf(2480));
        given(data.closePrice()).willReturn(BigDecimal.valueOf(2525));
        given(data.prdyCtrt()).willReturn(BigDecimal.valueOf(1.5)); // 정상적인 등락률
        given(data.volume()).willReturn(10000L);

        BenchmarkPriceDataWrapper wrapper = new BenchmarkPriceDataWrapper(type, data);

        // when
        BenchmarkPrice result = processor.process(wrapper);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getChangeRate()).isEqualByComparingTo("1.5");
    }
}
