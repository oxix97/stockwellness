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
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});
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
        when(env.getActiveProfiles()).thenReturn(new String[]{"dev"});
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

    @Test
    @DisplayName("필드가 10개를 초과할 경우 여러 섹션으로 나누어 생성한다")
    void build_WhenFieldsExceed10_SplitsIntoMultipleSections() {
        // given
        when(env.getActiveProfiles()).thenReturn(new String[]{"local"});
        Map<String, String> details = new java.util.LinkedHashMap<>();
        for (int i = 1; i <= 15; i++) {
            details.put("Key" + i, "Value" + i);
        }
        
        NotificationContext context = NotificationContext.builder()
            .title("많은 필드 테스트")
            .content("Content")
            .details(details)
            .type(NotificationContext.NotificationType.INFO)
            .build();

        // when
        Map<String, Object> result = slackMessageBuilder.build(context);

        // then
        List<Map<String, Object>> attachments = (List<Map<String, Object>>) result.get("attachments");
        List<Map<String, Object>> blocks = (List<Map<String, Object>>) attachments.get(0).get("blocks");
        
        long sectionWithFieldsCount = blocks.stream()
            .filter(b -> b.get("type").equals("section") && b.containsKey("fields"))
            .count();
        
        assertThat(sectionWithFieldsCount).isEqualTo(2); // 15 fields should be split into 10 + 5
        
        List<Map<String, Object>> firstSectionFields = (List<Map<String, Object>>) blocks.stream()
            .filter(b -> b.get("type").equals("section") && b.containsKey("fields"))
            .findFirst().get().get("fields");
        assertThat(firstSectionFields).hasSize(10);
    }
}
