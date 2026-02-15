package org.stockwellness.global.error;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.stockwellness.domain.member.exception.MemberDomainException;
import org.stockwellness.domain.portfolio.exception.PortfolioDomainException;
import org.stockwellness.domain.stock.exception.StockDomainException;
import org.stockwellness.global.error.exception.BusinessException;

import static org.stockwellness.global.error.ErrorCode.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PortfolioDomainException.class)
    public ProblemDetail handlePortfolioDomainException(PortfolioDomainException e) {
        return createProblemDetail(e.getErrorCode());
    }

    @ExceptionHandler(MemberDomainException.class)
    public ProblemDetail handleMemberDomainException(MemberDomainException e) {
        return createProblemDetail(e.getErrorCode());
    }

    @ExceptionHandler(StockDomainException.class)
    public ProblemDetail handleStockDomainException(StockDomainException e) {
        return createProblemDetail(e.getErrorCode());
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
        log.error("Unexpected error occurred: ", e);
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