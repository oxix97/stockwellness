package org.stockwellness.domain.portfolio.strategy;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LumpSumModel(BigDecimal initialAmount) implements CashFlowModel {
    @Override
    public BigDecimal getInvestmentAmount(LocalDate date, boolean isFirstDay) {
        return isFirstDay ? initialAmount : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getInitialAmount() {
        return initialAmount;
    }
}
