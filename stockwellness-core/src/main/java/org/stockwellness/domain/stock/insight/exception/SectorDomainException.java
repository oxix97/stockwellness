package org.stockwellness.domain.stock.insight.exception;

import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

public class SectorDomainException extends BusinessException {
    public SectorDomainException(ErrorCode errorCode) {
        super(errorCode);
    }
}
