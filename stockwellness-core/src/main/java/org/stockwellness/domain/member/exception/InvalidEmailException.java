package org.stockwellness.domain.member.exception;

public class InvalidEmailException extends MemberDomainException {
    public InvalidEmailException(String message) {
        super(message);
    }
}
