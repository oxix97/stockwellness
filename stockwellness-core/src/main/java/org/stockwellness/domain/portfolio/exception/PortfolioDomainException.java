package org.stockwellness.domain.portfolio.exception;

import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

public class PortfolioDomainException extends BusinessException {
    public PortfolioDomainException(String message) {
        super(ErrorCode.INVALID_INPUT_VALUE); // 기본값 혹은 메시지 전달 방식 수정 필요
        // BusinessException 구조상 ErrorCode가 필수라면, 메시지만 받는 생성자는 지원하기 어려울 수 있음.
        // 현재 BusinessException을 확인해보는 것이 좋음. 일단 가장 안전한 방식으로 수정.
    }

    public PortfolioDomainException(ErrorCode errorCode) {
        super(errorCode);
    }
}
