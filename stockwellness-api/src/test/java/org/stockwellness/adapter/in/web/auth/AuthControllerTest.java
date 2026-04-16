package org.stockwellness.adapter.in.web.auth;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.support.annotation.MockMember;
import org.stockwellness.application.port.in.auth.dto.LoginRequest;
import org.stockwellness.application.port.in.auth.dto.ReissueRequest;
import org.stockwellness.application.port.in.auth.AuthUseCase;
import org.stockwellness.application.port.in.auth.command.LoginCommand;
import org.stockwellness.application.port.in.auth.result.LoginResult;
import org.stockwellness.application.port.in.auth.result.ReissueResult;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.fixture.AuthFixture;
import org.stockwellness.support.RestDocsSupport;
import org.springframework.restdocs.payload.FieldDescriptor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Auth 컨트롤러 통합 테스트 (RestDocs)")
class AuthControllerTest extends RestDocsSupport {

    @MockitoBean
    private AuthUseCase authUseCase;

    @Nested
    @DisplayName("로그인 API")
    class Login {
        @Test
        @DisplayName("로그인 성공 시 토큰을 반환한다")
        void login_success() throws Exception {
            // given
            LoginRequest request = AuthFixture.createLoginRequest();
            LoginResult result = new LoginResult(AuthFixture.ACCESS_TOKEN, AuthFixture.REFRESH_TOKEN, 1L, AuthFixture.EMAIL, AuthFixture.NICKNAME, LocalDate.now());

            given(authUseCase.login(any(LoginCommand.class))).willReturn(result);

            List<FieldDescriptor> responseFields = new ArrayList<>(commonResponseFields());
            responseFields.addAll(List.of(
                    fieldWithPath("data.accessToken").description("액세스 토큰"),
                    fieldWithPath("data.refreshToken").description("리프레시 토큰"),
                    fieldWithPath("data.memberId").description("회원 ID"),
                    fieldWithPath("data.email").description("이메일"),
                    fieldWithPath("data.nickname").description("닉네임"),
                    fieldWithPath("data.joinedDate").description("가입 일자")
            ));

            // when & then
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andDo(document("auth-login",
                            resource(ResourceSnippetParameters.builder()
                                    .tag("Auth")
                                    .summary("로그인")
                                    .description("소셜 로그인 정보를 통해 액세스 토큰 및 리프레시 토큰을 발급받습니다.")
                                    .requestSchema(Schema.schema("LoginRequest"))
                                    .responseSchema(Schema.schema("LoginResponse"))
                                    .requestFields(
                                            fieldWithPath("email").description("이메일"),
                                            fieldWithPath("nickname").description("닉네임"),
                                            fieldWithPath("loginType").description("로그인 타입 (GOOGLE, KAKAO, NAVER)")
                                    )
                                    .responseFields(responseFields)
                                    .build())
                    ));
        }

        @Test
        @DisplayName("카카오 로그인 성공 시 토큰을 반환한다")
        void login_kakao_success() throws Exception {
            // given
            LoginRequest request = new LoginRequest("ieejo716@naver.com", "chan", LoginType.KAKAO);
            LoginResult result = new LoginResult(AuthFixture.ACCESS_TOKEN, AuthFixture.REFRESH_TOKEN, 1L, "ieejo716@naver.com", "chan", LocalDate.now());

            given(authUseCase.login(any(LoginCommand.class))).willReturn(result);

            List<FieldDescriptor> responseFields = new ArrayList<>(commonResponseFields());
            responseFields.addAll(List.of(
                    fieldWithPath("data.accessToken").description("액세스 토큰"),
                    fieldWithPath("data.refreshToken").description("리프레시 토큰"),
                    fieldWithPath("data.memberId").description("회원 ID"),
                    fieldWithPath("data.email").description("이메일"),
                    fieldWithPath("data.nickname").description("닉네임"),
                    fieldWithPath("data.joinedDate").description("가입 일자")
            ));

            // when & then
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andDo(document("auth-login-kakao",
                            resource(ResourceSnippetParameters.builder()
                                    .tag("Auth")
                                    .summary("카카오 로그인")
                                    .description("카카오 소셜 로그인 정보를 통해 액세스 토큰 및 리프레시 토큰을 발급받습니다.")
                                    .requestSchema(Schema.schema("LoginRequest"))
                                    .responseSchema(Schema.schema("LoginResponse"))
                                    .requestFields(
                                            fieldWithPath("email").description("이메일"),
                                            fieldWithPath("nickname").description("닉네임"),
                                            fieldWithPath("loginType").description("로그인 타입 (GOOGLE, KAKAO, NAVER)")
                                    )
                                    .responseFields(responseFields)
                                    .build())
                    ));
        }
    }

    @Nested
    @DisplayName("토큰 재발급 API")
    class Reissue {
        @Test
        @DisplayName("유효한 리프레시 토큰으로 액세스 토큰을 재발급한다")
        void reissue_success() throws Exception {
            // given
            ReissueRequest request = AuthFixture.createReissueRequest();
            ReissueResult result = new ReissueResult("new.access.token", "new.refresh.token");

            given(authUseCase.reissue(any())).willReturn(result);

            List<FieldDescriptor> responseFields = new ArrayList<>(commonResponseFields());
            responseFields.addAll(List.of(
                    fieldWithPath("data.accessToken").description("새로운 액세스 토큰"),
                    fieldWithPath("data.refreshToken").description("새로운 리프레시 토큰")
            ));

            // when & then
            mockMvc.perform(post("/api/v1/auth/reissue")
                            .with(csrf())
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andDo(document("auth-reissue",
                            resource(ResourceSnippetParameters.builder()
                                    .tag("Auth")
                                    .summary("토큰 재발급")
                                    .description("리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.")
                                    .requestSchema(Schema.schema("ReissueRequest"))
                                    .responseSchema(Schema.schema("ReissueResponse"))
                                    .requestFields(
                                            fieldWithPath("refreshToken").description("리프레시 토큰")
                                    )
                                    .responseFields(responseFields)
                                    .build())
                    ));
        }
    }

    @Nested
    @DisplayName("로그아웃 API")
    class Logout {
        @Test
        @MockMember(id = 1L)
        @DisplayName("로그아웃 성공 시 200 OK를 반환한다")
        void logout_success() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/auth/logout")
                            .with(csrf())
                            .header("Authorization", "Bearer {ACCESS_TOKEN}")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andDo(document("auth-logout",
                            resource(ResourceSnippetParameters.builder()
                                    .tag("Auth")
                                    .summary("로그아웃")
                                    .description("서버에서 리프레시 토큰을 삭제합니다.")
                                    .requestHeaders(
                                            headerWithName("Authorization").description("Bearer Access Token")
                                    )
                                    .responseSchema(Schema.schema("EmptyDataResponse"))
                                    .responseFields(commonResponseFieldsWithNoData())
                                    .build())
                    ));
            
            verify(authUseCase).logout(1L);
        }
    }
}
