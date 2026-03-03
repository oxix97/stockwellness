package org.stockwellness.adapter.in.web.sector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.application.port.in.stock.SectorInsightUseCase;
import org.stockwellness.application.port.in.stock.result.SectorComparisonResult;
import org.stockwellness.application.port.in.stock.result.SectorDetailResult;
import org.stockwellness.application.port.in.stock.result.SectorRankingResult;
import org.stockwellness.application.port.in.stock.result.SectorSupplyResult;
import org.stockwellness.domain.stock.MarketType;

import org.stockwellness.global.util.DateUtil;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sectors")
public class SectorDashboardController {

    private final SectorInsightUseCase sectorInsightUseCase;

    /**
     * [기능 26] 등락률 상위 섹터 조회
     */
    @GetMapping("/ranking/fluctuation")
    public ResponseEntity<List<SectorRankingResult>> getTopSectorsByFluctuation(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) MarketType marketType,
            @RequestParam(defaultValue = "10") int limit) {
        
        LocalDate targetDate = DateUtil.getTodayIfNull(date);
        return ResponseEntity.ok(sectorInsightUseCase.getTopSectorsByFluctuation(targetDate, marketType, limit));
    }

    /**
     * [기능 27] 수급 상위 섹터 조회
     */
    @GetMapping("/ranking/supply")
    public ResponseEntity<List<SectorSupplyResult>> getTopSectorsBySupply(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) MarketType marketType,
            @RequestParam(defaultValue = "10") int limit) {
        
        LocalDate targetDate = DateUtil.getTodayIfNull(date);
        return ResponseEntity.ok(sectorInsightUseCase.getTopSectorsBySupply(targetDate, marketType, limit));
    }

    /**
     * [기능 31] 섹터 vs 시장 비교 분석 조회
     */
    @GetMapping("/{sectorCode}/comparison")
    public ResponseEntity<SectorComparisonResult> compareWithMarket(
            @PathVariable String sectorCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = DateUtil.getTodayIfNull(date);
        return ResponseEntity.ok(sectorInsightUseCase.compareWithMarket(sectorCode, targetDate));
    }

    /**
     * [기능 30] 섹터 상세 및 진단 정보 조회
     */
    @GetMapping("/{sectorCode}")
    public ResponseEntity<SectorDetailResult> getSectorDetail(
            @PathVariable String sectorCode,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = DateUtil.getTodayIfNull(date);
        return ResponseEntity.ok(sectorInsightUseCase.getSectorDetail(sectorCode, targetDate));
    }
}
