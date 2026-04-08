package org.stockwellness.application.port.in.batch;

import org.stockwellness.application.port.out.stock.SectorApiDto;
import org.stockwellness.domain.stock.insight.SectorInsight;

public interface SectorEodSyncUseCase {

    SectorEodResult syncSector(SectorSyncCommand command);

    SectorEodResult enrichAiOpinion(SectorAiAnalysisCommand command);

    record SectorSyncCommand(SectorApiDto sectorApiDto) {
    }

    record SectorAiAnalysisCommand(SectorInsight sectorInsight) {
    }

    record SectorEodResult(SectorInsight sectorInsight) {
    }
}
