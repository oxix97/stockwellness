package org.stockwellness.application.port.in.batch;

import org.stockwellness.application.port.out.stock.SectorApiDto;
import org.stockwellness.domain.stock.insight.SectorInsight;

import java.time.LocalDate;
import java.util.List;

public interface SectorEodSyncUseCase {

    void prepareSync(LocalDate targetDate, List<String> sectorCodes);

    SectorEodResult syncSector(SectorSyncCommand command);

    SectorEodResult enrichAiOpinion(SectorAiAnalysisCommand command);

    record SectorSyncCommand(SectorApiDto sectorApiDto) {
    }

    record SectorAiAnalysisCommand(SectorInsight sectorInsight) {
    }

    record SectorEodResult(SectorInsight sectorInsight) {
    }
}
