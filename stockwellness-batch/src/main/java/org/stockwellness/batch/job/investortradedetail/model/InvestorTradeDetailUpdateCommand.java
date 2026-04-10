package org.stockwellness.batch.job.investortradedetail.model;

import org.stockwellness.domain.stock.price.InvestorSupplyDemand;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestorTradeDetailUpdateCommand(
        Long stockId,
        LocalDate baseDate,
        InvestorSupplyDemand supplyDemand
) {
    public InvestorTradeDetailUpdateCommand(Long stockId, LocalDate baseDate, Long instQty, Long frgnQty, BigDecimal instAmt, BigDecimal frgnAmt) {
        this(stockId, baseDate, new InvestorSupplyDemand(
                instAmt, frgnAmt, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, instAmt.add(frgnAmt),
                instQty, frgnQty, 0L, 0L, 0L, instQty + frgnQty
        ));
    }

    public Long netInstitutionalBuyingQty() {
        return supplyDemand.getInstitutionQty();
    }

    public Long netForeignBuyingQty() {
        return supplyDemand.getForeignQty();
    }

    public BigDecimal netInstitutionalBuyingAmt() {
        return supplyDemand.getInstitutionAmt();
    }

    public BigDecimal netForeignBuyingAmt() {
        return supplyDemand.getForeignAmt();
    }
}
