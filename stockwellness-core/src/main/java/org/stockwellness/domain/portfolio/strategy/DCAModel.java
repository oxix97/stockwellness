package org.stockwellness.domain.portfolio.strategy;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DCAModel(BigDecimal monthlyAmount) implements CashFlowModel {
    @Override
    public BigDecimal getInvestmentAmount(LocalDate date, boolean isFirstDay) {
        if (isFirstDay) return monthlyAmount;
        // Check if it's the first business day of the month
        // In this simple model, we assume the first day provided for each month is the deposit day.
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getInitialAmount() {
        return monthlyAmount;
    }
}
