package org.stockwellness.application.sector.step.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.external.kis.dto.SectorApiDto;
import org.stockwellness.application.port.in.batch.SectorEodSyncUseCase;
import org.stockwellness.domain.stock.insight.SectorInsight;
@Component
@RequiredArgsConstructor
public class SectorInsightItemProcessor implements ItemProcessor<SectorApiDto, SectorInsight> {

    private final SectorEodSyncUseCase sectorEodSyncUseCase;

    @Override
    public SectorInsight process(SectorApiDto apiDto) {
        return sectorEodSyncUseCase.syncSector(new SectorEodSyncUseCase.SectorSyncCommand(apiDto)).sectorInsight();
    }
}
