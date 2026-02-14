package org.stockwellness.adapter.in.web.watchlist.dto;

import org.stockwellness.application.port.out.watchlist.StockDataPort;
import org.stockwellness.domain.watchlist.WatchlistItem;

import java.math.BigDecimal;
import java.util.List;

public record WatchlistItemListResponse(
        String groupName,
        List<WatchlistItemDetail> items
) {
    public record WatchlistItemDetail(
            String isinCode,
            String name,
            BigDecimal currentPrice,
            BigDecimal fluctuationRate,
            BigDecimal rsi,
            String rsiStatus,
            String aiInsight
    ) {
        public static WatchlistItemDetail of(WatchlistItem item, StockDataPort.StockWellnessDetail detail) {
            if (detail == null) {
                return new WatchlistItemDetail(
                        item.getStock().getStandardCode(),
                        item.getStock().getName(),
                        null, null, null, "데이터 없음", "데이터가 부족하여 AI 분석을 제공할 수 없습니다."
                );
            }
            return new WatchlistItemDetail(
                    item.getStock().getStandardCode(),
                    item.getStock().getName(),
                    detail.currentPrice(),
                    detail.fluctuationRate(),
                    detail.rsi(),
                    detail.rsiStatus(),
                    detail.aiInsight()
            );
        }
    }
}