package org.stockwellness.domain.portfolio.exception;

import org.stockwellness.global.error.ErrorCode;

public class PortfolioNotFoundException extends PortfolioDomainException {
    public PortfolioNotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
