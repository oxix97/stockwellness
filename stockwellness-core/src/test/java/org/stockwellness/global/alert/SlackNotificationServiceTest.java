package org.stockwellness.global.alert;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class SlackNotificationServiceTest {

    private SlackNotificationService slackNotificationService;

    @Mock
    private SlackAlertProperties properties;

    @Mock
    private SlackMessageBuilder messageBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        slackNotificationService = new SlackNotificationService(properties, messageBuilder, restClient);
    }

    @Test
    @DisplayName("Webhook URL이 없으면 전송하지 않는다")
    void sendNotification_WhenWebhookUrlIsNull_DoesNotSend() {
        // given
        when(properties.webhookUrl()).thenReturn(null);
        NotificationContext context = NotificationContext.builder().title("Title").build();

        // when
        slackNotificationService.sendNotification(context);

        // then
        verifyNoInteractions(messageBuilder);
        verifyNoInteractions(restClient);
    }

    @Test
    @DisplayName("잘못된 Webhook URL이면 전송하지 않는다")
    void sendNotification_WhenWebhookUrlIsInvalid_DoesNotSend() {
        // given
        when(properties.webhookUrl()).thenReturn("invalid-url");
        NotificationContext context = NotificationContext.builder().title("Title").build();

        // when
        slackNotificationService.sendNotification(context);

        // then
        verifyNoInteractions(messageBuilder);
        verifyNoInteractions(restClient);
    }

    @Test
    @DisplayName("정상적인 상황에서 알림을 전송한다")
    void sendNotification_WhenValid_SendsNotification() {
        // given
        String url = "https://hooks.slack.com/services/test";
        when(properties.webhookUrl()).thenReturn(url);
        NotificationContext context = NotificationContext.builder().title("Title").content("Content").build();
        Map<String, Object> payload = Map.of("text", "payload");
        when(messageBuilder.build(context)).thenReturn(payload);

        // when
        slackNotificationService.sendNotification(context);

        // then
        verify(restClient.post().uri(URI.create(url)).body(payload).retrieve()).toBodilessEntity();
    }
}
