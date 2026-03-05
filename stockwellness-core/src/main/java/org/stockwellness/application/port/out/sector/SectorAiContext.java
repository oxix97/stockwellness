package org.stockwellness.application.port.out.sector;

import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.analysis.TrendStatus;
import org.stockwellness.domain.stock.insight.LeadingStock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 섹터 AI 분석을 위한 데이터 컨텍스트 (Port Out)
 */
public record SectorAiContext(
        String sectorName,
        String sectorCode,
        MarketType marketType,
        LocalDate baseDate,
        BigDecimal indexPrice,
        BigDecimal fluctuationRate,
        Long netForeignBuy,
        Long netInstBuy,
        Integer foreignDays,
        Integer instDays,
        TrendStatus trendStatus,
        BigDecimal rsi,
        boolean isOverheated,
        List<LeadingStock> leadingStocks
) {}
