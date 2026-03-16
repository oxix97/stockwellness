package org.stockwellness.application.port.in.watchlist.dto;

import org.stockwellness.application.port.out.watchlist.StockDataPort;
import org.stockwellness.domain.watchlist.WatchlistItem;

import java.math.BigDecimal;
import java.util.List;

public record WatchlistItemListResponse(
        String groupName,
        List<WatchlistItemDetail> items
) {
    public record WatchlistItemDetail(
            String ticker,
            String name,
            BigDecimal currentPrice,
            BigDecimal fluctuationRate,
            String note,
            BigDecimal rsi,
            String rsiStatus,
            String aiInsight
    ) {
        public static WatchlistItemDetail of(WatchlistItem item, StockDataPort.StockWellnessDetail detail) {
            if (detail == null) {
                return new WatchlistItemDetail(
                        item.getTicker(),
                        item.getStock().getName(),
                        null, null, item.getNote(), null, "데이터 없음", "데이터가 부족하여 AI 분석을 제공할 수 없습니다."
                );
            }
            return new WatchlistItemDetail(
                    item.getTicker(),
                    item.getStock().getName(),
                    detail.currentPrice(),
                    detail.fluctuationRate(),
                    item.getNote(),
                    detail.rsi(),
                    detail.rsiStatus(),
                    detail.aiInsight()
            );
        }
    }
}
