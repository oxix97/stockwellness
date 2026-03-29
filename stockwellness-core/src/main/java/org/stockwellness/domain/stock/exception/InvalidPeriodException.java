package org.stockwellness.domain.stock.exception;

import org.stockwellness.global.error.ErrorCode;

public class InvalidPeriodException extends StockDomainException {
    public InvalidPeriodException(String label) {
        super(ErrorCode.INVALID_INPUT_VALUE);
    }
}
