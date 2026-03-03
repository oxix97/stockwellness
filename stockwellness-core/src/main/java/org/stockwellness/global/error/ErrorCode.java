package org.stockwellness.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.CONFLICT;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 공통
    INVALID_INPUT_VALUE(BAD_REQUEST, "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    RESOURCE_NOT_FOUND(NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // 인증/인가
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    EXPIRED_JWT(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    INVALID_JWT(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰을 찾을 수 없습니다."),

    // 회원
    MEMBER_NOT_FOUND(NOT_FOUND, "회원을 찾을 수 없습니다."),
    DUPLICATE_EMAIL(CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(CONFLICT, "이미 사용 중인 닉네임입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),

    //포트폴리오
    INVALID_PORTFOLIO_TOTAL_PIECE(BAD_REQUEST, "포트폴리오 총 조각 수는 1개 이상, 8개 이하여야 합니다."),
    INVALID_ITEM_PIECE_COUNT(BAD_REQUEST, "각 종목은 최소 1조각 이상이어야 합니다."),
    DUPLICATE_PORTFOLIO_NAME(CONFLICT, "이미 사용 중인 포트폴리오 이름입니다."),
    PORTFOLIO_NOT_FOUND(NOT_FOUND, "포트폴리오를 찾을 수 없습니다."),

    // 관심 종목 (Watchlist)
    WATCHLIST_GROUP_LIMIT_EXCEEDED(BAD_REQUEST, "관심 그룹은 최대 10개까지 생성할 수 있습니다."),

    WATCHLIST_ITEM_LIMIT_EXCEEDED(BAD_REQUEST, "관심 종목은 한 그룹당 최대 50개까지 추가할 수 있습니다."),
    DUPLICATE_WATCHLIST_ITEM(CONFLICT, "이미 그룹에 등록된 종목입니다."),

    // 주식
    STOCK_NOT_FOUND(NOT_FOUND, "종목을 찾을 수 없습니다."),
    PRICE_DATA_NOT_FOUND(NOT_FOUND, "해당 기간의 시세 데이터를 찾을 수 없습니다."),

    // 섹터 (Sector)
    SECTOR_NOT_FOUND(NOT_FOUND, "섹터를 찾을 수 없습니다."),
    SECTOR_DATA_NOT_FOUND(NOT_FOUND, "섹터 상세 정보를 찾을 수 없습니다."),
    SECTOR_HISTORY_NOT_FOUND(NOT_FOUND, "섹터 이력 데이터를 찾을 수 없습니다."),
    SECTOR_SYNC_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "섹터 데이터 동기화 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    public int getStatusCode() {
        return status.value();
    }
}