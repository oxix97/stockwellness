package org.stockwellness.application.service.sector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.in.web.sector.dto.SectorDiagnosisResponse;
import org.stockwellness.adapter.in.web.sector.dto.SectorRankingResponse;
import org.stockwellness.application.port.in.sector.GetSectorDashboardUseCase;
import org.stockwellness.domain.stock.insight.SectorInsight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SectorDashboardService implements GetSectorDashboardUseCase {

    // [Port] DB 조회를 담당하는 아웃바운드 포트
    private final QuerySectorInsightPort querySectorInsightPort;

    /**
     * [기능 26, 27] 섹터 랭킹 및 수급 조회 (Redis 캐싱 적용)
     * - cacheNames: Redis에 저장될 key prefix (예: sectorRanking::2026-02-23)
     */
    @Override
    @Cacheable(cacheNames = "sectorRanking", key = "#targetDate.toString() + '_' + #limit")
    public List<SectorRankingResponse> getTopSectorsByFluctuation(LocalDate targetDate, int limit) {
        log.info("Cache Miss! DB에서 {} 일자 섹터 랭킹을 조회합니다. (limit: {})", targetDate, limit);

        List<SectorInsight> insights = querySectorInsightPort.findTopSectorsByDate(targetDate, limit);

        // Entity -> DTO 변환 (Java 21 Stream 활용)
        return insights.stream()
                .map(insight -> new SectorRankingResponse(
                        insight.getSectorCode(),
                        resolveSectorName(insight.getSectorCode()), // 프론트엔드용 이름 변환
                        insight.getSectorIndexCurrentPrice(),
                        insight.getAvgFluctuationRate(),
                        insight.getNetForeignBuyAmount(),
                        insight.getNetInstBuyAmount(),
                        insight.getForeignConsecutiveBuyDays(),
                        insight.getInstConsecutiveBuyDays()
                ))
                .collect(Collectors.toList());
    }

    /**
     * [기능 30] 특정 섹터 과열/침체 진단 (Redis 캐싱 적용)
     */
    @Override
    @Cacheable(cacheNames = "sectorDiagnosis", key = "#sectorCode + '_' + #targetDate.toString()")
    public SectorDiagnosisResponse getSectorDiagnosis(String sectorCode, LocalDate targetDate) {
        log.info("Cache Miss! DB에서 {} 섹터의 {} 일자 진단 지표를 조회합니다.", sectorCode, targetDate);

        SectorInsight insight = querySectorInsightPort.findBySectorCodeAndDate(sectorCode, targetDate)
                .orElseThrow(() -> new IllegalArgumentException("해당 일자의 섹터 데이터가 없습니다."));

        BigDecimal rsi14 = insight.getTechnicalIndicators().getRsi14();
        
        return new SectorDiagnosisResponse(
                insight.getSectorCode(),
                insight.getBaseDate(),
                rsi14,
                determineRsiStatus(rsi14), // 퀀트 로직: 과열/침체 판단
                insight.getTechnicalIndicators().getMa20(),
                insight.getTechnicalIndicators().getMa60()
        );
    }

    // ==========================================
    // 내부 퀀트 및 유틸리티 로직
    // ==========================================

    /**
     * [퀀트 로직] RSI 수치에 따른 시장 과열/침체 진단
     * (일반적인 웰즈 와일더의 기준선 70 / 30 적용)
     */
    private String determineRsiStatus(BigDecimal rsi) {
        if (rsi == null) return "데이터 부족";
        
        double rsiValue = rsi.doubleValue();
        if (rsiValue >= 70.0) {
            return "과열 (Overbought)";
        } else if (rsiValue <= 30.0) {
            return "침체 (Oversold)";
        } else {
            return "중립 (Neutral)";
        }
    }

    /**
     * 섹터 코드 -> 섹터명 변환 (추후 공통 코드 메모리 캐시 등으로 고도화 필요)
     */
    private String resolveSectorName(String sectorCode) {
        // 임시 하드코딩 (실제로는 Enum이나 공통 코드 테이블 참조)
        return switch (sectorCode) {
            case "0001" -> "조선/해운";
            case "0002" -> "반도체";
            default -> "기타 업종 (" + sectorCode + ")";
        };
    }
}