package org.stockwellness.batch.job.sector.step;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.stockwellness.application.port.out.stock.MarketIndexPort;
import org.stockwellness.application.port.out.stock.SectorDailyDetailPort;
import org.stockwellness.adapter.batch.sector.step.reader.SectorMarketIndexItemReader;
import org.stockwellness.domain.stock.insight.MarketIndex;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SectorMarketIndexItemReaderTest {

    @InjectMocks
    private SectorMarketIndexItemReader reader;

    @Mock
    private MarketIndexPort marketIndexPort;

    @Mock
    private SectorDailyDetailPort sectorDailyDetailPort;

    @Test
    @DisplayName("0001 초과 2000 미만 indexCode만 KIS 수집 대상으로 읽는다")
    void read_filtersEligibleIndexCodes() {
        ReflectionTestUtils.setField(reader, "targetDateStr", "2026-04-09");
        given(marketIndexPort.findAll()).willReturn(List.of(
                MarketIndex.of("0001", "종합"),
                MarketIndex.of("0002", "대형주"),
                MarketIndex.of("1014", "금융"),
                MarketIndex.of("2000", "제외"),
                MarketIndex.of("ABC", "비숫자")
        ));

        MarketIndex first = reader.read();
        MarketIndex second = reader.read();
        MarketIndex third = reader.read();

        assertThat(first.getIndexCode()).isEqualTo("0002");
        assertThat(second.getIndexCode()).isEqualTo("1014");
        assertThat(third).isNull();
        verify(sectorDailyDetailPort).deleteByBaseDateAndSectorCodeNotIn(
                java.time.LocalDate.of(2026, 4, 9),
                List.of("0002", "1014")
        );
    }

    @Test
    @DisplayName("compact targetDate도 동일하게 파싱한다")
    void read_supportsCompactTargetDate() {
        ReflectionTestUtils.setField(reader, "targetDateStr", "20260409");
        given(marketIndexPort.findAll()).willReturn(List.of(
                MarketIndex.of("0002", "대형주")
        ));

        MarketIndex first = reader.read();
        MarketIndex second = reader.read();

        assertThat(first.getIndexCode()).isEqualTo("0002");
        assertThat(second).isNull();
        verify(sectorDailyDetailPort).deleteByBaseDateAndSectorCodeNotIn(
                java.time.LocalDate.of(2026, 4, 9),
                List.of("0002")
        );
    }
}
