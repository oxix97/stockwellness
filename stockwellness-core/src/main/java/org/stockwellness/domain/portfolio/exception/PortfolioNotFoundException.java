package org.stockwellness.domain.portfolio.exception;

import org.stockwellness.global.error.ErrorCode;

public class PortfolioNotFoundException extends PortfolioDomainException {
    public PortfolioNotFoundException() {
        super(ErrorCode.PORTFOLIO_NOT_FOUND);
    }
}
