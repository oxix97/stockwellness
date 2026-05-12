package org.stockwellness.adapter.out.external.kis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class KisOverseasIndexDailyPriceTest {

    @Test
    @DisplayName("해외 지수 일별 시세 DTO 매핑 및 BenchmarkPriceData 인터페이스 구현 검증")
    void mappingTest() {
        // given
        KisOverseasIndexDailyPrice dto = new KisOverseasIndexDailyPrice(
                "20260401", // stckBsopDate
                "5200.50",  // ovrsNmixPrpr (close)
                "5180.20",  // ovrsNmixOprc (open)
                "5210.80",  // ovrsNmixHgpr (high)
                "5175.10",  // ovrsNmixLwpr (low)
                "1234567",  // acmlVol
                "0"         // modYn
        );

        // when
        BenchmarkPriceData data = (BenchmarkPriceData) dto;

        // then
        assertThat(data.baseDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(data.closePrice()).isEqualByComparingTo("5200.50");
        assertThat(data.openPrice()).isEqualByComparingTo("5180.20");
        assertThat(data.highPrice()).isEqualByComparingTo("5210.80");
        assertThat(data.lowPrice()).isEqualByComparingTo("5175.10");
        assertThat(data.volume()).isEqualTo(1234567L);
        assertThat(data.prdyVrss()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(data.prdyCtrt()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
