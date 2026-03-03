package org.stockwellness.adapter.in.web.sector.dto;

import java.math.BigDecimal;

// 1. 섹터 랭킹 및 수급 현황 응답 DTO (기능 26, 27)
public record SectorRankingResponse(
        String sectorCode,
        String sectorName, // (참고: 프론트엔드 표시를 위해 공통 코드맵에서 변환 필요)
        BigDecimal currentPrice,
        BigDecimal avgFluctuationRate,
        Long netForeignBuyAmount,
        Long netInstBuyAmount,
        Integer foreignConsecutiveBuyDays,
        Integer instConsecutiveBuyDays
) {
}