package org.stockwellness.adapter.in.web.stock.dto;

public record StockAnalysisResponse(
        String isinCode,
        String context
) {
    public static StockAnalysisResponse of(String isinCode, String context) {
        return new StockAnalysisResponse(isinCode, context);
    }
}
