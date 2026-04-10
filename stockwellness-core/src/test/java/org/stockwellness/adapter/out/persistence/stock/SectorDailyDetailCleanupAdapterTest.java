package org.stockwellness.adapter.out.persistence.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.adapter.out.persistence.stock.repository.SectorDailyDetailRepository;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SectorDailyDetailCleanupAdapterTest {

    @InjectMocks
    private SectorDailyDetailPersistenceAdapter adapter;

    @Mock
    private SectorDailyDetailRepository sectorDailyDetailRepository;

    @Test
    @DisplayName("정리 대상 코드 목록이 있으면 baseDate + not in 삭제를 사용한다")
    void deleteByBaseDateAndSectorCodeNotIn_withEligibleCodes() {
        LocalDate baseDate = LocalDate.of(2026, 4, 9);

        adapter.deleteByBaseDateAndSectorCodeNotIn(baseDate, List.of("0002", "1014"));

        verify(sectorDailyDetailRepository).deleteByBaseDateAndSectorCodeNotIn(baseDate, List.of("0002", "1014"));
    }

    @Test
    @DisplayName("정리 대상 코드 목록이 비어 있으면 해당 날짜 전체를 삭제한다")
    void deleteByBaseDateAndSectorCodeNotIn_withoutEligibleCodes() {
        LocalDate baseDate = LocalDate.of(2026, 4, 9);

        adapter.deleteByBaseDateAndSectorCodeNotIn(baseDate, List.of());

        verify(sectorDailyDetailRepository).deleteByBaseDate(baseDate);
    }
}
