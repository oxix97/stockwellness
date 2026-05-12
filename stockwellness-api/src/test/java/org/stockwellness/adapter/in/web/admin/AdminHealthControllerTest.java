package org.stockwellness.adapter.in.web.admin;

import java.util.Map;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.support.RestDocsSupport;
import org.stockwellness.support.annotation.MockMember;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Admin Health API 통합 테스트")
class AdminHealthControllerTest extends RestDocsSupport {

    @MockitoBean
    private HealthEndpoint healthEndpoint;

    @Test
    @MockMember(id = 1L, role = "ADMIN")
    @DisplayName("시스템 상태 조회: DB, Redis, Kafka 연결 상태를 확인한다")
    void get_health() throws Exception {
        // given
        CompositeHealth mockHealth = mock(CompositeHealth.class);
        given(mockHealth.getStatus()).willReturn(Status.UP);
        given(mockHealth.getComponents()).willReturn(Map.of(
                "db", Health.up().build(),
                "redis", Health.up().build(),
                "kafka", Health.up().build()
        ));

        given(healthEndpoint.health()).willReturn(mockHealth);

        // when & then
        mockMvc.perform(get("/api/v1/admin/health")
                        .header("Authorization", "Bearer {ADMIN_TOKEN}")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("S000"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andDo(document("admin-health",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Admin")
                                .summary("시스템 헬스 체크")
                                .description("DB, Redis, Kafka의 연결 상태를 통합 조회합니다.")
                                .responseFields(
                                        fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                        fieldWithPath("status").type(NUMBER).description("HTTP 상태 코드"),
                                        fieldWithPath("code").type(STRING).description("비즈니스 상세 코드"),
                                        fieldWithPath("message").type(STRING).description("응답 메시지"),
                                        fieldWithPath("data.status").type(STRING).description("전체 상태 (UP/DOWN)"),
                                        fieldWithPath("data.components.db").type(STRING).description("데이터베이스 상태"),
                                        fieldWithPath("data.components.redis").type(STRING).description("Redis 캐시 상태"),
                                        fieldWithPath("data.components.kafka").type(STRING).description("Kafka 메시지 브로커 상태"),
                                        fieldWithPath("timestamp").type(STRING).description("응답 시간"),
                                        fieldWithPath("traceId").type(STRING).description("에러 추적 ID").optional(),
                                        fieldWithPath("errors").type(ARRAY).description("상세 에러 리스트").optional()
                                )
                                .build())
                ));
    }
    
    private <T> T mock(Class<T> type) {
        return Mockito.mock(type);
    }
}
