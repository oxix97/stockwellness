package org.stockwellness.adapter.in;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.stockwellness.application.service.AuthService;
import org.stockwellness.support.RestDocsSupport;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TestControllerTest extends RestDocsSupport {

    @Autowired
    AuthService authService;

    @Test
    @DisplayName("스프링 부트 테스트")
    void test1() throws Exception {
        mockMvc.perform(get("/api/v1/auth/test")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("health-check", // 식별자 (build/generated-snippets/health-check 생성됨)
                        resource(ResourceSnippetParameters.builder()
                                .tag("System") // Swagger 태그 (그룹핑)
                                .summary("시스템 상태 확인")
                                .description("서버가 정상적으로 동작 중인지 확인합니다.")
                                .responseFields(
                                        fieldWithPath("status").description("상태 코드 (ok)"),
                                        fieldWithPath("service").description("서비스명")
                                )
                                .build()
                        )
                ));
    }
}