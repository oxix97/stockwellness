package org.stockwellness.domain.stock.exception;

import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

public class StockPriceException extends BusinessException {
    public StockPriceException(ErrorCode errorCode) {
        super(errorCode);
    }
}
