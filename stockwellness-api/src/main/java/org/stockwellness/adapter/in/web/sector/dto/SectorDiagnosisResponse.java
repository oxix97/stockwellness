package org.stockwellness.adapter.in.web.sector.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// 2. 섹터 과열 진단 응답 DTO (기능 30)
public record SectorDiagnosisResponse(
        String sectorCode,
        LocalDate baseDate,
        BigDecimal rsi14,
        String rsiStatus,     // 예: "과열(Overbought)", "침체(Oversold)", "중립(Neutral)"
        BigDecimal ma20,      // 20일선 (단기 추세 확인용)
        BigDecimal ma60       // 60일선 (중기 추세 확인용)
) {}