package org.stockwellness.domain.stock.exception;

import org.stockwellness.global.error.ErrorCode;

public class InvalidStockCodeException extends StockDomainException {
    public InvalidStockCodeException(String message) {
        super(ErrorCode.INVALID_INPUT_VALUE); // 메시지는 로깅 등으로 활용 가능하나, ErrorCode 기반이므로 코드 전달
    }
}
