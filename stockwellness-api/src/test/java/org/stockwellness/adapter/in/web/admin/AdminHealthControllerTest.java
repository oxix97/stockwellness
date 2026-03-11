package org.stockwellness.adapter.in.web.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.support.RestDocsSupport;
import org.stockwellness.support.annotation.MockMember;

import java.util.Map;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
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
        // CompositeHealth is not public constructor or easily buildable in some versions.
        // Let's use simple Health with details if Composite casting fails in controller, 
        // OR better, mock HealthEndpoint to return a mock HealthComponent.
        
        org.springframework.boot.actuate.health.CompositeHealth mockHealth = mock(org.springframework.boot.actuate.health.CompositeHealth.class);
        given(mockHealth.getStatus()).willReturn(org.springframework.boot.actuate.health.Status.UP);
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
                .andDo(document("admin-health",
                        resource(com.epages.restdocs.apispec.ResourceSnippetParameters.builder()
                                .tag("Admin")
                                .summary("시스템 헬스 체크")
                                .description("DB, Redis, Kafka의 연결 상태를 통합 조회합니다.")
                                .responseFields(
                                        fieldWithPath("status").description("전체 상태 (UP/DOWN)"),
                                        fieldWithPath("components.db").description("데이터베이스 상태"),
                                        fieldWithPath("components.redis").description("Redis 캐시 상태"),
                                        fieldWithPath("components.kafka").description("Kafka 메시지 브로커 상태")
                                )
                                .build())
                ));
    }
    
    private <T> T mock(Class<T> type) {
        return org.mockito.Mockito.mock(type);
    }
}
