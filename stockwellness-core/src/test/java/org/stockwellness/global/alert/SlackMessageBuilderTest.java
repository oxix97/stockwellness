package org.stockwellness.global.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
class SlackMessageBuilderTest {

    private SlackMessageBuilder slackMessageBuilder;

    @Mock
    private Environment env;

    @BeforeEach
    void setUp() {
        slackMessageBuilder = new SlackMessageBuilder(env);
    }

    @Test
    @DisplayName("성공 알림 메시지 구조 검증 (Option A)")
    void build_SuccessNotification_ReturnsValidStructure() {
        // given
        when(env.getProperty("spring.profiles.active", "local")).thenReturn("prod");
        NotificationContext context = NotificationContext.builder()
            .title("배치 작업 완료")
            .content("데이터 동기화가 성공적으로 완료되었습니다.")
            .type(NotificationContext.NotificationType.SUCCESS)
            .build();

        // when
        Map<String, Object> result = slackMessageBuilder.build(context);

        // then
        assertThat(result).containsKey("attachments");
        List<Map<String, Object>> attachments = (List<Map<String, Object>>) result.get("attachments");
        assertThat(attachments).hasSize(1);

        Map<String, Object> attachment = attachments.get(0);
        assertThat(attachment.get("color")).isEqualTo("#2EB67D"); // SUCCESS color

        List<Map<String, Object>> blocks = (List<Map<String, Object>>) attachment.get("blocks");
        assertThat(blocks).isNotEmpty();

        // Header check
        Map<String, Object> header = blocks.get(0);
        assertThat(header.get("type")).isEqualTo("header");
        Map<String, Object> headerText = (Map<String, Object>) header.get("text");
        assertThat(headerText.get("text")).isEqualTo("✅ [PROD] 배치 작업 완료");
    }

    @Test
    @DisplayName("에러 알림 메시지 구조 및 필드 검증")
    void build_ErrorNotification_ContainsTraceAndContextInfo() {
        // given
        when(env.getProperty("spring.profiles.active", "local")).thenReturn("dev");
        NotificationContext context = NotificationContext.builder()
            .title("API 오류 발생")
            .content("내부 서버 오류가 발생했습니다.")
            .traceId("trace-123")
            .time("2024-05-20 10:00:00")
            .userId("user-456")
            .url("/api/v1/test")
            .exceptionType("RuntimeException")
            .stackTrace("NullPointerException at logic...")
            .type(NotificationContext.NotificationType.ERROR)
            .build();

        // when
        Map<String, Object> result = slackMessageBuilder.build(context);

        // then
        List<Map<String, Object>> attachments = (List<Map<String, Object>>) result.get("attachments");
        Map<String, Object> attachment = attachments.get(0);
        assertThat(attachment.get("color")).isEqualTo("#E01E5A"); // ERROR color

        List<Map<String, Object>> blocks = (List<Map<String, Object>>) attachment.get("blocks");
        
        // Fields check (Section with fields)
        Map<String, Object> fieldsSection = blocks.stream()
            .filter(b -> b.get("type").equals("section") && b.containsKey("fields"))
            .findFirst()
            .orElseThrow();
        
        List<Map<String, Object>> fields = (List<Map<String, Object>>) fieldsSection.get("fields");
        assertThat(fields).extracting(f -> f.get("text").toString())
            .containsExactlyInAnyOrder(
                "*Trace ID:*\n`trace-123`",
                "*Time:*\n2024-05-20 10:00:00",
                "*User ID:*\nuser-456",
                "*URL:*\n/api/v1/test"
            );

        // Stack trace check
        boolean hasStackTrace = blocks.stream()
            .filter(b -> b.get("type").equals("section") && b.containsKey("text"))
            .anyMatch(b -> {
                Map<String, Object> textObj = (Map<String, Object>) b.get("text");
                return textObj.get("text").toString().contains("Stack Trace");
            });
        assertThat(hasStackTrace).isTrue();
    }
}
