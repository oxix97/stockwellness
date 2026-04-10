package org.stockwellness.batch.job.benchmarkprice.step.reader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.BenchmarkPriceData;
import org.stockwellness.batch.job.benchmarkprice.model.BenchmarkPriceDataWrapper;
import org.stockwellness.domain.stock.BenchmarkType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BenchmarkPriceDataReaderTest {

    @Mock
    private KisDailyPriceAdapter kisDailyPriceAdapter;

    @Test
    @DisplayName("모든 지수 응답이 비면 배치를 실패시킨다")
    void read_throwsWhenAllBenchmarksAreEmpty() {
        given(kisDailyPriceAdapter.fetchIndexDailyPrices(any(), any(), any())).willReturn(Collections.emptyList());
        given(kisDailyPriceAdapter.fetchOverseasIndexDailyPrices(any(), any(), any())).willReturn(Collections.emptyList());

        BenchmarkPriceDataReader reader = new BenchmarkPriceDataReader(
                kisDailyPriceAdapter,
                LocalDate.of(2026, 4, 5),
                LocalDate.of(2026, 4, 5)
        );

        assertThatThrownBy(reader::read)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("지수 시세 동기화 결과가 비어 있습니다")
                .hasMessageContaining("attemptedBenchmarks=")
                .hasMessageContaining("emptyCount=");
    }

    @Test
    @DisplayName("한 지수라도 데이터가 있으면 정상적으로 읽고 종료한다")
    void read_returnsDataWhenAtLeastOneBenchmarkExists() {
        BenchmarkPriceData sample = new TestBenchmarkPriceData(LocalDate.of(2026, 4, 3), new BigDecimal("2500.00"));

        given(kisDailyPriceAdapter.fetchIndexDailyPrices(any(), any(), any()))
                .willReturn(Collections.emptyList());
        given(kisDailyPriceAdapter.fetchIndexDailyPrices(eq(BenchmarkType.KOSPI.getTicker()), any(), any()))
                .willReturn(List.of(sample));
        given(kisDailyPriceAdapter.fetchOverseasIndexDailyPrices(any(), any(), any()))
                .willReturn(Collections.emptyList());

        BenchmarkPriceDataReader reader = new BenchmarkPriceDataReader(
                kisDailyPriceAdapter,
                LocalDate.of(2026, 4, 3),
                LocalDate.of(2026, 4, 3)
        );

        BenchmarkPriceDataWrapper first = reader.read();
        BenchmarkPriceDataWrapper second = reader.read();

        assertThat(first.type()).isEqualTo(BenchmarkType.KOSPI);
        assertThat(first.data().closePrice()).isEqualByComparingTo("2500.00");
        assertThat(second).isNull();
    }

    @Test
    @DisplayName("일부 지수 조회가 실패해도 다른 지수 데이터가 있으면 계속 진행한다")
    void read_continuesWhenSomeBenchmarksFailButOthersSucceed() {
        BenchmarkPriceData sample = new TestBenchmarkPriceData(LocalDate.of(2026, 4, 3), new BigDecimal("2500.00"));

        given(kisDailyPriceAdapter.fetchIndexDailyPrices(any(), any(), any()))
                .willThrow(new IllegalStateException("국내 지수 조회 실패"));
        given(kisDailyPriceAdapter.fetchOverseasIndexDailyPrices(any(), any(), any()))
                .willReturn(Collections.emptyList());
        given(kisDailyPriceAdapter.fetchOverseasIndexDailyPrices(eq(BenchmarkType.S_P_500.getTicker()), any(), any()))
                .willReturn(List.of(sample));

        BenchmarkPriceDataReader reader = new BenchmarkPriceDataReader(
                kisDailyPriceAdapter,
                LocalDate.of(2026, 4, 3),
                LocalDate.of(2026, 4, 3)
        );

        BenchmarkPriceDataWrapper first = reader.read();
        BenchmarkPriceDataWrapper second = reader.read();

        assertThat(first).isNotNull();
        assertThat(first.type().isOverseas()).isTrue();
        assertThat(first.data().closePrice()).isEqualByComparingTo("2500.00");
        assertThat(second).isNull();
    }

    private record TestBenchmarkPriceData(LocalDate baseDate, BigDecimal closePrice) implements BenchmarkPriceData {

        @Override
        public BigDecimal openPrice() {
            return closePrice;
        }

        @Override
        public BigDecimal highPrice() {
            return closePrice;
        }

        @Override
        public BigDecimal lowPrice() {
            return closePrice;
        }

        @Override
        public BigDecimal prdyVrss() {
            return BigDecimal.ZERO;
        }

        @Override
        public BigDecimal prdyCtrt() {
            return BigDecimal.ZERO;
        }

        @Override
        public Long volume() {
            return 0L;
        }
    }
}
