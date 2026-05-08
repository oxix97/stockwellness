package org.stockwellness.application.port.in.stock.result;

import org.stockwellness.domain.stock.insight.LeadingStock;
import org.stockwellness.domain.stock.insight.SectorAiOpinion;
import org.stockwellness.domain.stock.insight.SectorTechnicalIndicators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SectorDetailResult(
    String sectorCode,
    String sectorName,
    LocalDate baseDate,
    BigDecimal currentPrice,
    BigDecimal fluctuationRate,
    SectorTechnicalIndicators technicalIndicators,
    boolean isOverheated,
    String diagnosisMessage,
    List<LeadingStock> leadingStocks,
    SectorAiOpinion aiOpinion // 추가
) {}
