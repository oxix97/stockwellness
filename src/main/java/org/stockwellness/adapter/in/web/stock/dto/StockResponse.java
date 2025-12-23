package org.stockwellness.adapter.in.web.stock.dto;

import org.stockwellness.domain.stock.Stock;

public record StockResponse(
        String isinCode,    // isinCd
        String ticker,      // srtnCd
        String name,        // itmsNm
        String marketType,  // mrktCtg
        Long totalShares    // 상장주식수
) {
    // Entity -> DTO 변환 팩토리 메서드
    public static StockResponse from(Stock stock) {
        return new StockResponse(
                stock.getIsinCode(),
                stock.getTicker(),
                stock.getName(),
                stock.getMarketType().name(),
                stock.getTotalShares()
        );
    }
}