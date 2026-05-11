package org.stockwellness.application.port.in.batch;

import java.time.LocalDate;
import java.util.List;

import org.stockwellness.adapter.out.external.kis.dto.SectorApiDto;
import org.stockwellness.domain.stock.insight.SectorInsight;

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
