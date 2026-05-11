package org.stockwellness.adapter.in.web.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.stockwellness.global.alert.SlackAlertService;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.GlobalExceptionHandler;
import org.stockwellness.global.error.exception.GlobalException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final SlackAlertService slackAlertService = mock(SlackAlertService.class);

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler(slackAlertService))
            .build();

    @Test
    @DisplayName("GlobalException 발생 시 표준 ApiResponse를 반환한다")
    void handleGlobalException_test() throws Exception {
        mockMvc.perform(get("/test/exception")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("M001"))
                .andExpect(jsonPath("$.message").value("회원을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    @DisplayName("입력값 검증 실패 시 FieldError 목록을 포함한 ApiResponse를 반환한다")
    void handleBindingException_test() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .content("{\"name\":\"\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("G001"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].reason").exists());
    }

    @Test
    @DisplayName("예상치 못한 500 에러 발생 시 Slack 알림을 트리거한다")
    void handleUnexpectedException_sendsSlackAlert() throws Exception {
        mockMvc.perform(get("/test/unexpected")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("G004"));

        verify(slackAlertService).sendInternalServerErrorAlert(anyString(), any(Exception.class));
    }

    @Test
    @DisplayName("4xx BusinessException 발생 시 Slack 알림을 전송하지 않는다")
    void handleBusinessException_doesNotSendSlackAlert() throws Exception {
        mockMvc.perform(get("/test/exception")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(slackAlertService, never()).sendInternalServerErrorAlert(anyString(), any());
    }

    @RestController
    static class TestController {
        @GetMapping("/test/exception")
        public void throwBusinessException() {
            throw new GlobalException(ErrorCode.MEMBER_NOT_FOUND);
        }

        @GetMapping("/test/unexpected")
        public void throwUnexpectedException() {
            throw new RuntimeException("예상치 못한 오류");
        }

        @PostMapping("/test/validation")
        public void testValidation(@RequestBody @Valid TestDto dto) {
        }
    }

    record TestDto(@NotBlank String name) {}
}
