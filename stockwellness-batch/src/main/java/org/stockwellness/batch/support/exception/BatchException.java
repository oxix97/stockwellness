package org.stockwellness.batch.support.exception;

import lombok.Getter;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

@Getter
public class BatchException extends BusinessException {
    public BatchException(ErrorCode errorCode) {
        super(errorCode);
    }
}
