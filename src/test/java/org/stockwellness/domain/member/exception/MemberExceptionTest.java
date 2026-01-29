package org.stockwellness.domain.member.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;

class MemberExceptionTest {

    @Test
    @DisplayName("MemberNotFoundException은 BusinessException을 상속하고 MEMBER_NOT_FOUND 에러 코드를 가진다")
    void memberNotFoundExceptionTest() {
        // given
        // When
        MemberNotFoundException exception = new MemberNotFoundException();

        // Then
        assertThat(exception).isInstanceOf(BusinessException.class);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("EmailDuplicateException은 BusinessException을 상속하고 DUPLICATE_EMAIL 에러 코드를 가진다")
    void emailDuplicateExceptionTest() {
        // given
        // When
        EmailDuplicateException exception = new EmailDuplicateException();

        // Then
        assertThat(exception).isInstanceOf(BusinessException.class);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    @DisplayName("NicknameDuplicateException은 BusinessException을 상속하고 DUPLICATE_NICKNAME 에러 코드를 가진다")
    void nicknameDuplicateExceptionTest() {
        // given
        // When
        NicknameDuplicateException exception = new NicknameDuplicateException();

        // Then
        assertThat(exception).isInstanceOf(BusinessException.class);
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
    }
}
