package org.stockwellness.domain.portfolio.exception;

import org.stockwellness.global.error.ErrorCode;

public class DuplicatePortfolioNameException extends PortfolioDomainException {
    public DuplicatePortfolioNameException() {
        super(ErrorCode.DUPLICATE_PORTFOLIO_NAME);
    }
}
