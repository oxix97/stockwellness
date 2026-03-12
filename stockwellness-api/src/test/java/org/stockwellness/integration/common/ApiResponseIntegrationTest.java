package org.stockwellness.integration.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.stockwellness.application.port.in.member.MemberUseCase;
import org.stockwellness.application.port.in.member.result.MemberResult;
import org.stockwellness.domain.member.MemberRole;
import org.stockwellness.domain.member.MemberStatus;
import org.stockwellness.domain.member.RiskLevel;
import org.stockwellness.support.annotation.MockMember;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiResponseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberUseCase memberUseCase;

    @Test
    @DisplayName("성공 응답 시 표준 포맷(data, timestamp)을 준수해야 한다")
    @MockMember(id = 1L)
    void success_response_format_test() throws Exception {
        MemberResult result = new MemberResult(
                1L, "test@example.com", "tester", MemberRole.USER, RiskLevel.MEDIUM,
                MemberStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now(),
                MemberResult.PortfolioSummaryResult.empty()
        );
        given(memberUseCase.getMember(anyLong())).willReturn(result);

        mockMvc.perform(get("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("에러 응답 시 표준 포맷(status, code, message, timestamp, traceId, errors)을 준수해야 한다")
    @MockMember(id = 1L)
    void error_response_format_test() throws Exception {
        // 존재하지 않는 포트폴리오 ID로 요청하여 BusinessException 유도
        mockMvc.perform(get("/api/v1/portfolios/9999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("P004"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.errors").isArray());
    }
}
