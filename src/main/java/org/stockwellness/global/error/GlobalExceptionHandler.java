package org.stockwellness.global.error;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.stockwellness.domain.portfolio.exception.PortfolioDomainException;
import org.stockwellness.global.error.exception.BusinessException;

import static org.stockwellness.global.error.ErrorCode.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PortfolioDomainException.class)
    public ProblemDetail handlePortfolioDomainException(PortfolioDomainException e) {
        return ProblemDetail.forStatusAndDetail(INVALID_INPUT_VALUE.getStatus(), e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException e) {
        return e.getProblemDetail();
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ProblemDetail handleExpiredJwt() {
        return createProblemDetail(EXPIRED_JWT);
    }

    @ExceptionHandler(JwtException.class)
    public ProblemDetail handleJwtException(JwtException e) {
        // 서명 오류, malformed 등은 모두 INVALID_JWT
        return createProblemDetail(INVALID_JWT);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        e.printStackTrace(); // 로깅은 나중에 Logback으로
        return createProblemDetail(INTERNAL_SERVER_ERROR);
    }

    private ProblemDetail createProblemDetail(ErrorCode errorCode) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                errorCode.getStatus(), errorCode.getMessage());
        pd.setTitle(errorCode.name());
        pd.setProperty("errorCode", errorCode.name());
        return pd;
    }
}