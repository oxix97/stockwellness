package org.stockwellness.domain.stock.exception;

import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

public class UnsupportedBenchmarkSeriesException extends BusinessException {
    public UnsupportedBenchmarkSeriesException() {
        super(ErrorCode.INVALID_INPUT_VALUE);
    }
}
