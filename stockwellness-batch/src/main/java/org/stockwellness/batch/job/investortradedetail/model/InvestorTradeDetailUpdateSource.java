package org.stockwellness.batch.job.investortradedetail.model;

public record InvestorTradeDetailUpdateSource(
        String ticker,
        String institutionalBuyingQtyText,
        String foreignBuyingQtyText,
        String institutionalBuyingAmtText,
        String foreignBuyingAmtText
) {
}
