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
import org.stockwellness.adapter.out.persistence.stock.repository.MarketIndexRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.SectorInsightRepository;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.insight.SectorIndicators;
import org.stockwellness.domain.stock.insight.SectorInsight;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SectorPersistenceAdapterTest {

    @InjectMocks
    private SectorPersistenceAdapter adapter;

    @Mock
    private SectorInsightRepository sectorInsightRepository;

    @Mock
    private MarketIndexRepository marketIndexRepository;

    @Test
    @DisplayName("saveAll은 동일 날짜 chunk를 선조회 후 일괄 저장한다")
    void saveAll_bulkUpsert() {
        LocalDate baseDate = LocalDate.of(2026, 4, 9);
        SectorInsight existing = SectorInsight.of("전기전자", "0029", MarketType.KOSPI, baseDate,
                SectorIndicators.of(BigDecimal.ONE, BigDecimal.ZERO, 1L, 1L, 1, 1), null, false);
        SectorInsight updated = SectorInsight.of("전기전자", "0029", MarketType.KOSPI, baseDate,
                SectorIndicators.of(BigDecimal.TEN, BigDecimal.ZERO, 2L, 2L, 2, 2), null, true);
        SectorInsight created = SectorInsight.of("반도체", "1010", MarketType.KOSDAQ, baseDate,
                SectorIndicators.of(BigDecimal.valueOf(20), BigDecimal.ZERO, 3L, 3L, 3, 3), null, false);

        given(sectorInsightRepository.findBySectorCodeInAndBaseDate(List.of("0029", "1010"), baseDate))
                .willReturn(List.of(existing));

        adapter.saveAll(List.of(updated, created));

        verify(sectorInsightRepository).findBySectorCodeInAndBaseDate(List.of("0029", "1010"), baseDate);
        verify(sectorInsightRepository).saveAll(anyList());
        verify(sectorInsightRepository).flush();
        verify(sectorInsightRepository, times(0)).findBySectorCodeAndBaseDate("0029", baseDate);
    }
}
