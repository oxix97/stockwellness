package org.stockwellness.domain.member.exception;

import org.stockwellness.global.error.ErrorCode;

public class InvalidEmailException extends MemberDomainException {
    public InvalidEmailException(String message) {
        super(message);
    }
}
