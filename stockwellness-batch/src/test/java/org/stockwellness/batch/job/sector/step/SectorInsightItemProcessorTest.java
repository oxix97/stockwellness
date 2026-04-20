package org.stockwellness.batch.job.sector.step;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.adapter.out.external.kis.dto.SectorApiDto;
import org.stockwellness.application.port.in.batch.SectorEodSyncUseCase;
import org.stockwellness.adapter.batch.sector.step.processor.SectorInsightItemProcessor;
import org.stockwellness.domain.stock.insight.SectorInsight;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SectorInsightItemProcessorTest {

    private final SectorEodSyncUseCase sectorEodSyncUseCase = mock(SectorEodSyncUseCase.class);
    private final SectorInsightItemProcessor processor = new SectorInsightItemProcessor(sectorEodSyncUseCase);

    @Test
    @DisplayName("SectorApiDto 처리 시 use case 호출 결과를 반환한다")
    void processDtoAndVerifyCaching() {
        LocalDate today = LocalDate.now();
        SectorApiDto apiDto = new SectorApiDto("001", "전기전자", today, BigDecimal.valueOf(2500), BigDecimal.valueOf(1.0), 1000L, 500L);

        SectorInsight expectedResult = mock(SectorInsight.class);
        when(sectorEodSyncUseCase.syncSector(any())).thenReturn(new SectorEodSyncUseCase.SectorEodResult(expectedResult));

        SectorInsight result1 = processor.process(apiDto);
        SectorInsight result2 = processor.process(apiDto);

        assertThat(result1).isEqualTo(expectedResult);
        assertThat(result2).isEqualTo(expectedResult);
        verify(sectorEodSyncUseCase, times(2)).syncSector(any());
    }
}
