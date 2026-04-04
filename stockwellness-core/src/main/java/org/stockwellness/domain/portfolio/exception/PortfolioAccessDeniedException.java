package org.stockwellness.domain.portfolio.exception;

import org.stockwellness.global.error.ErrorCode;

public class PortfolioAccessDeniedException extends PortfolioDomainException {
    public PortfolioAccessDeniedException() {
        super(ErrorCode.PORTFOLIO_ACCESS_DENIED);
    }
}
