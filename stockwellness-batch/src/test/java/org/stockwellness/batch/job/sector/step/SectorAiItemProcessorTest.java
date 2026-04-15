package org.stockwellness.batch.job.sector.step;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.application.port.in.batch.SectorEodSyncUseCase;
import org.stockwellness.application.sector.step.processor.SectorAiItemProcessor;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.insight.SectorIndicators;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SectorAiItemProcessorTest {

    private final SectorEodSyncUseCase sectorEodSyncUseCase = mock(SectorEodSyncUseCase.class);
    private final SectorAiItemProcessor processor = new SectorAiItemProcessor(sectorEodSyncUseCase);

    @Test
    @DisplayName("AI processor는 use case 위임 결과를 반환한다")
    void resolveTrendStatusRegular() {
        SectorInsight insight = createDefaultInsight();
        when(sectorEodSyncUseCase.enrichAiOpinion(any())).thenReturn(new SectorEodSyncUseCase.SectorEodResult(insight));

        SectorInsight result = processor.process(insight);

        assertThat(result).isSameAs(insight);
        verify(sectorEodSyncUseCase).enrichAiOpinion(any());
    }

    @Test
    @DisplayName("AI 분석 결과가 그대로 전달되는지 확인")
    void processUpdateOpinion() {
        SectorInsight insight = createDefaultInsight();
        when(sectorEodSyncUseCase.enrichAiOpinion(any())).thenReturn(new SectorEodSyncUseCase.SectorEodResult(insight));

        SectorInsight result = processor.process(insight);

        assertThat(result).isSameAs(insight);
    }

    private SectorInsight createDefaultInsight() {
        return SectorInsight.of(
                "테스트", "001", MarketType.KOSPI, LocalDate.now(),
                SectorIndicators.of(BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, 0, 0),
                TechnicalIndicators.empty(),
                false
        );
    }
}
