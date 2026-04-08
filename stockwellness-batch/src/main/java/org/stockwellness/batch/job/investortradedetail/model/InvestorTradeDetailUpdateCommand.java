package org.stockwellness.batch.job.investortradedetail.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestorTradeDetailUpdateCommand(
        Long stockId,
        LocalDate baseDate,
        BigDecimal netInstitutionalBuyingAmt,
        BigDecimal netForeignBuyingAmt
) {
}
