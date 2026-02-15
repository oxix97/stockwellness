package org.stockwellness.domain.member.exception;

import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

public class NicknameDuplicateException extends BusinessException {
    public NicknameDuplicateException() {
        super(ErrorCode.DUPLICATE_NICKNAME);
    }
}
