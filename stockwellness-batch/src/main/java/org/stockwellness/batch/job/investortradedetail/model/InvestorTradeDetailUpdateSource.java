package org.stockwellness.batch.job.investortradedetail.model;

public record InvestorTradeDetailUpdateSource(
        String ticker,
        String institutionalBuyingQtyText,
        String foreignBuyingQtyText,
        String pensionFundBuyingQtyText,
        String trustBuyingQtyText,
        String etcCorpBuyingQtyText,
        String totalNetBuyingQtyText,
        String institutionalBuyingAmtText,
        String foreignBuyingAmtText,
        String pensionFundBuyingAmtText,
        String trustBuyingAmtText,
        String etcCorpBuyingAmtText,
        String totalNetBuyingAmtText
) {
    public InvestorTradeDetailUpdateSource(String ticker, String instQty, String frgnQty, String instAmt, String frgnAmt) {
        this(ticker, instQty, frgnQty, "0", "0", "0", "0", instAmt, frgnAmt, "0", "0", "0", "0");
    }
}
