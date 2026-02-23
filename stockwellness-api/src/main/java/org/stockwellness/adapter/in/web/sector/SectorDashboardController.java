package org.stockwellness.adapter.in.web.sector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.adapter.in.web.sector.dto.SectorDiagnosisResponse;
import org.stockwellness.adapter.in.web.sector.dto.SectorRankingResponse;
import org.stockwellness.application.port.in.sector.GetSectorDashboardUseCase;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sectors")
public class SectorDashboardController {

    private final GetSectorDashboardUseCase getSectorDashboardUseCase;

    /**
     * [기능 26, 27] 오늘의 섹터 랭킹 및 수급 조회
     */
    @GetMapping("/ranking")
    public ResponseEntity<List<SectorRankingResponse>> getSectorRanking(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "10") int limit) {
        
        // 날짜가 없으면 오늘 날짜(또는 가장 최근 영업일) 기준
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        
        List<SectorRankingResponse> response = getSectorDashboardUseCase.getTopSectorsByFluctuation(targetDate, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * [기능 30] 특정 섹터 과열/침체 진단 (RSI 등)
     */
    @GetMapping("/{sectorCode}/diagnosis")
    public ResponseEntity<SectorDiagnosisResponse> getSectorDiagnosis(
            @PathVariable String sectorCode,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        
        SectorDiagnosisResponse response = getSectorDashboardUseCase.getSectorDiagnosis(sectorCode, targetDate);
        return ResponseEntity.ok(response);
    }
}