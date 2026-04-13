package org.stockwellness.application.port.out.stock;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestorTradingSnapshot(
        LocalDate baseDate,
        BigDecimal closePrice,
        BigDecimal priceChangeRate,
        Long volume,
        Long netInstitutionalBuyingQty,
        Long netForeignBuyingQty,
        Long netPersonBuyingQty,
        BigDecimal netInstitutionalBuyingAmt,
        BigDecimal netForeignBuyingAmt,
        BigDecimal netPersonBuyingAmt
) {
}
