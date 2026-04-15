package org.stockwellness.batch.job.sector.step;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.stockwellness.application.port.out.stock.SectorDailyDetailSnapshot;
import org.stockwellness.application.port.out.stock.SectorDataPort;
import org.stockwellness.application.sector.step.processor.SectorDailyDetailItemProcessor;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.SectorDailyDetail;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SectorDailyDetailItemProcessorTest {

    @InjectMocks
    private SectorDailyDetailItemProcessor processor;

    @Mock
    private SectorDataPort sectorDataPort;

    @Test
    @DisplayName("0001 이하 또는 2000 이상 indexCode는 KIS 호출 없이 건너뛴다")
    void process_skipsNonEligibleIndexCode() {
        ReflectionTestUtils.setField(processor, "targetDateStr", "2026-04-09");

        SectorDailyDetail result = processor.process(MarketIndex.of("2000", "제외"));

        assertThat(result).isNull();
        verify(sectorDataPort, never()).fetchTodaySectorDetail("2000", LocalDate.of(2026, 4, 9));
    }

    @Test
    @DisplayName("0001 초과 2000 미만 indexCode는 기존처럼 KIS 호출 후 상세를 생성한다")
    void process_callsKisForEligibleIndexCode() {
        ReflectionTestUtils.setField(processor, "targetDateStr", "2026-04-09");
        given(sectorDataPort.fetchTodaySectorDetail("0013", LocalDate.of(2026, 4, 9)))
                .willReturn(snapshot("0013", LocalDate.of(2026, 4, 9)));

        SectorDailyDetail result = processor.process(MarketIndex.of("0013", "전기·전자"));

        assertThat(result).isNotNull();
        assertThat(result.getSectorCode()).isEqualTo("0013");
        verify(sectorDataPort).fetchTodaySectorDetail("0013", LocalDate.of(2026, 4, 9));
    }

    private SectorDailyDetailSnapshot snapshot(String indexCode, LocalDate baseDate) {
        return new SectorDailyDetailSnapshot(
                indexCode,
                baseDate,
                new BigDecimal("1000.12"),
                new BigDecimal("12.34"),
                "2",
                new BigDecimal("1.23"),
                100L,
                90L,
                1000L,
                900L,
                new BigDecimal("990.00"),
                new BigDecimal("1010.00"),
                new BigDecimal("980.00"),
                10,
                1,
                2,
                3,
                0,
                new BigDecimal("1100.00"),
                new BigDecimal("-9.08"),
                baseDate,
                new BigDecimal("800.00"),
                new BigDecimal("25.01"),
                baseDate.minusMonths(1),
                100L,
                200L,
                new BigDecimal("33.33"),
                new BigDecimal("66.67"),
                100L,
                10L,
                20L
        );
    }
}
