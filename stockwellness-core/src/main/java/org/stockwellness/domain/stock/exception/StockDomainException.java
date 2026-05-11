package org.stockwellness.domain.stock.exception;

import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

public class StockDomainException extends BusinessException {
    public StockDomainException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public StockDomainException(String message) {
        super(ErrorCode.INVALID_INPUT_VALUE); // 기본값 설정 필요 시 조정
    }
}
