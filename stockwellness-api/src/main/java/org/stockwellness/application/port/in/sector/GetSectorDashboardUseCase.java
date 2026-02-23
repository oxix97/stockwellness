package org.stockwellness.application.port.in.sector;

import org.stockwellness.adapter.in.web.sector.dto.SectorDiagnosisResponse;
import org.stockwellness.adapter.in.web.sector.dto.SectorRankingResponse;

import java.time.LocalDate;
import java.util.List;

public interface GetSectorDashboardUseCase {
    
    // 등락률 기준 상위 N개 섹터 조회
    List<SectorRankingResponse> getTopSectorsByFluctuation(LocalDate targetDate, int limit);
    
    // 특정 섹터의 퀀트 지표 및 과열 상태 진단
    SectorDiagnosisResponse getSectorDiagnosis(String sectorCode, LocalDate targetDate);
}