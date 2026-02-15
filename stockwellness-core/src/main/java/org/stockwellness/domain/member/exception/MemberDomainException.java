package org.stockwellness.domain.member.exception;

import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

public class MemberDomainException extends BusinessException {
    public MemberDomainException(String message) {
        super(ErrorCode.INVALID_INPUT_VALUE);
    }

    public MemberDomainException(ErrorCode errorCode) {
        super(errorCode);
    }
}
