package org.stockwellness.application.port.out.stock;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * KIS 업종 일별 상세 응답을 도메인 친화적으로 옮긴 원천 스냅샷.
 */
public record SectorDailyDetailSnapshot(
        String sectorCode,
        LocalDate baseDate,
        BigDecimal currentPrice,
        BigDecimal changeAmount,
        String changeSign,
        BigDecimal changeRate,
        Long accumulatedVolume,
        Long previousVolume,
        Long accumulatedTradingAmount,
        Long previousTradingAmount,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        Integer risingIssueCount,
        Integer upperLimitIssueCount,
        Integer steadyIssueCount,
        Integer fallingIssueCount,
        Integer lowerLimitIssueCount,
        BigDecimal yearlyHighPrice,
        BigDecimal yearlyHighRate,
        LocalDate yearlyHighDate,
        BigDecimal yearlyLowPrice,
        BigDecimal yearlyLowRate,
        LocalDate yearlyLowDate,
        Long totalAskResidualQuantity,
        Long totalBidResidualQuantity,
        BigDecimal sellResidualRate,
        BigDecimal buyResidualRate,
        Long netBuyResidualQuantity,
        Long netForeignBuyAmount,
        Long netInstBuyAmount
) {
}
