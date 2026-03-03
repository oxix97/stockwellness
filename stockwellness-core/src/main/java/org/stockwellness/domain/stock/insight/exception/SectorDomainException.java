package org.stockwellness.domain.stock.insight.exception;

import org.stockwellness.global.error.exception.BusinessException;
import org.stockwellness.global.error.ErrorCode;

public class SectorDomainException extends BusinessException {
    public SectorDomainException(ErrorCode errorCode) {
        super(errorCode);
    }
}
