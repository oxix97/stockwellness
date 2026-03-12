package org.stockwellness.adapter.in.web.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.GlobalExceptionHandler;
import org.stockwellness.global.error.exception.GlobalException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    @DisplayName("GlobalException 발생 시 표준 ErrorResponse를 반환한다")
    void handleGlobalException_test() throws Exception {
        mockMvc.perform(get("/test/exception")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("M001"))
                .andExpect(jsonPath("$.message").value("회원을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    @DisplayName("입력값 검증 실패 시 FieldError 목록을 포함한 ErrorResponse를 반환한다")
    void handleBindingException_test() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .content("{\"name\":\"\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("G001"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].reason").exists());
    }

    @RestController
    static class TestController {
        @GetMapping("/test/exception")
        public void throwException() {
            throw new GlobalException(ErrorCode.MEMBER_NOT_FOUND);
        }

        @PostMapping("/test/validation")
        public void testValidation(@RequestBody @Valid TestDto dto) {
        }
    }

    record TestDto(@NotBlank String name) {}
}
