package org.stockwellness.domain.portfolio.exception;

import org.stockwellness.global.error.ErrorCode;

public class InvalidPortfolioException extends PortfolioDomainException {
    public InvalidPortfolioException() {
        super(ErrorCode.INVALID_INPUT_VALUE);
    }
}
