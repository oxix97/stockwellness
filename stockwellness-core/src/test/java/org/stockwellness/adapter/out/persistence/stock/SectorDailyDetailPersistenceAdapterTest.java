package org.stockwellness.adapter.out.persistence.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.adapter.out.persistence.stock.repository.SectorDailyDetailRepository;
import org.stockwellness.application.port.out.stock.SectorDailyDetailSnapshot;
import org.stockwellness.domain.stock.insight.SectorDailyDetail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SectorDailyDetailPersistenceAdapterTest {

    @InjectMocks
    private SectorDailyDetailPersistenceAdapter adapter;

    @Mock
    private SectorDailyDetailRepository sectorDailyDetailRepository;

    @Test
    @DisplayName("saveAll은 동일 날짜 chunk를 선조회 후 일괄 저장한다")
    void saveAll_bulkUpsert() {
        LocalDate baseDate = LocalDate.of(2026, 4, 9);
        SectorDailyDetail existing = newDetail("0029", "전기전자", baseDate, 10L, 20L);
        SectorDailyDetail updated = newDetail("0029", "전기전자", baseDate, 30L, 40L);
        SectorDailyDetail created = newDetail("1010", "반도체", baseDate, 50L, 60L);

        given(sectorDailyDetailRepository.findBySectorCodeInAndBaseDate(List.of("0029", "1010"), baseDate))
                .willReturn(List.of(existing));

        adapter.saveAll(List.of(updated, created));

        verify(sectorDailyDetailRepository).findBySectorCodeInAndBaseDate(List.of("0029", "1010"), baseDate);
        verify(sectorDailyDetailRepository).saveAll(anyList());
        verify(sectorDailyDetailRepository).flush();
        verify(sectorDailyDetailRepository, times(0)).findBySectorCodeAndBaseDate("0029", baseDate);
    }

    private SectorDailyDetail newDetail(String sectorCode, String sectorName, LocalDate baseDate, long foreign, long inst) {
        return SectorDailyDetail.of(
                sectorCode,
                sectorName,
                new SectorDailyDetailSnapshot(
                        sectorCode,
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
                        foreign,
                        inst
                )
        );
    }
}
