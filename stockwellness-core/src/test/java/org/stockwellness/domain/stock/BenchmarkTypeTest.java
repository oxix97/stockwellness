package org.stockwellness.domain.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BenchmarkType 테스트")
class BenchmarkTypeTest {

    @Test
    @DisplayName("기본 시뮬레이션 비교군 목록은 모바일 시뮬레이션 기본 순서를 유지한다")
    void default_simulation_benchmarks() {
        assertThat(BenchmarkType.defaultSimulationBenchmarks())
                .containsExactly(
                        BenchmarkType.KOSPI_200,
                        BenchmarkType.S_P_500,
                        BenchmarkType.NASDAQ_100,
                        BenchmarkType.DOW_JONES
                );

        assertThat(BenchmarkType.defaultSimulationBenchmarkTickers())
                .containsExactly("2001", "SPX", "NDX", ".DJI")
                .doesNotHaveDuplicates();
    }
}
