package org.stockwellness.adapter.in.web.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.stockwellness.support.annotation.MockMember;
import org.stockwellness.adapter.in.web.member.dto.UpdateMemberRequest;
import org.stockwellness.application.port.in.member.MemberUseCase;
import org.stockwellness.application.port.in.member.result.MemberResult;
import org.stockwellness.domain.member.MemberRole;
import org.stockwellness.domain.member.MemberStatus;
import org.stockwellness.domain.member.RiskLevel;
import org.stockwellness.support.RestDocsSupport;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberControllerTest extends RestDocsSupport {

    @MockitoBean
    private MemberUseCase memberUseCase;

    @Test
    @DisplayName("내 정보 조회 API")
    @MockMember(id = 1L)
    void getMember() throws Exception {
        MemberResult result = new MemberResult(
                1L,
                "test@example.com",
                "tester",
                MemberRole.USER,
                RiskLevel.MEDIUM,
                MemberStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                MemberResult.PortfolioSummaryResult.empty()
        );

        given(memberUseCase.getMember(anyLong())).willReturn(result);

        mockMvc.perform(get("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("member-get-me"));
    }

    @Test
    @DisplayName("내 정보 수정 API")
    @MockMember(id = 1L)
    void updateMember() throws Exception {
        UpdateMemberRequest request = new UpdateMemberRequest("newNickname", RiskLevel.HIGH);

        mockMvc.perform(put("/api/v1/members/me")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("member-update-me"));
    }

    @Test
    @DisplayName("회원 탈퇴 API")
    @MockMember(id = 1L)
    void deactivateMember() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("member-delete-me"));
    }
}
