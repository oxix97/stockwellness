package org.stockwellness.adapter.batch.sector.step.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.batch.SectorEodSyncUseCase;
import org.stockwellness.domain.stock.insight.SectorInsight;
@Component
@RequiredArgsConstructor
public class SectorAiItemProcessor implements ItemProcessor<SectorInsight, SectorInsight> {

    private final SectorEodSyncUseCase sectorEodSyncUseCase;

    @Override
    public SectorInsight process(SectorInsight insight) {
        return sectorEodSyncUseCase.enrichAiOpinion(
                new SectorEodSyncUseCase.SectorAiAnalysisCommand(insight)
        ).sectorInsight();
    }
}
