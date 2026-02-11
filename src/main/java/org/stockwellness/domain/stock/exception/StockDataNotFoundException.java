package org.stockwellness.domain.stock.exception;

import org.stockwellness.global.error.ErrorCode;

public class StockDataNotFoundException extends StockDomainException {
    public StockDataNotFoundException(String isinCode) {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
