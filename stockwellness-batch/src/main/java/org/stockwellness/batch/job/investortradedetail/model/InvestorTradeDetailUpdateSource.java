package org.stockwellness.batch.job.investortradedetail.model;

public record InvestorTradeDetailUpdateSource(
        String ticker,
        String institutionalBuyingAmtText,
        String foreignBuyingAmtText
) {
}
