package org.stockwellness.global.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 성공 응답 코드를 정의하는 열거형입니다.
 * S로 시작하는 코드를 사용하며, HTTP 상태 코드와 메시지를 포함합니다.
 */
@Getter
@RequiredArgsConstructor
public enum SuccessCode {

    // Common (S)
    OK(HttpStatus.OK, "S000", "요청이 성공적으로 처리되었습니다."),
    CREATED(HttpStatus.CREATED, "S001", "리소스가 성공적으로 생성되었습니다."),
    ACCEPTED(HttpStatus.ACCEPTED, "S002", "요청이 접수되었습니다."),
    NO_CONTENT(HttpStatus.NO_CONTENT, "S003", "처리가 완료되었으며 반환할 데이터가 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    public int getStatusCode() {
        return status.value();
    }
}
