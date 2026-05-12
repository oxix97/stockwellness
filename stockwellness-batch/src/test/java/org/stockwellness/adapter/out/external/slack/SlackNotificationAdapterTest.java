package org.stockwellness.adapter.out.external.slack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.global.alert.NotificationContext;
import org.stockwellness.global.alert.SlackNotificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SlackNotificationAdapterTest {

    @Mock
    private SlackNotificationService slackNotificationService;

    private SlackNotificationAdapter slackNotificationAdapter;

    @BeforeEach
    void setUp() {
        slackNotificationAdapter = new SlackNotificationAdapter(slackNotificationService);
    }

    @Test
    @DisplayName("성공 알림은 SUCCESS 타입으로 전송한다")
    void sendSuccessNotification() {
        // given
        String title = "Batch Success";
        String content = "Total: 100\nSuccess: 100";

        // when
        assertDoesNotThrow(() -> slackNotificationAdapter.send(title, content));

        // then
        ArgumentCaptor<NotificationContext> contextCaptor = ArgumentCaptor.forClass(NotificationContext.class);
        verify(slackNotificationService).sendNotification(contextCaptor.capture());
        
        NotificationContext context = contextCaptor.getValue();
        assertThat(context.type()).isEqualTo(NotificationContext.NotificationType.SUCCESS);
        assertThat(context.title()).isEqualTo(title);
        assertThat(context.details()).containsEntry("Total", "100");
        assertThat(context.details()).containsEntry("Success", "100");
    }

    @Test
    @DisplayName("실패 알림은 ERROR 타입으로 전송한다")
    void sendFailureNotification() {
        // given
        String title = "🔴 [Batch Failure] Sync Job";
        String content = "상태: FAILED\n에러메시지: Network Timeout";

        // when
        assertDoesNotThrow(() -> slackNotificationAdapter.send(title, content));

        // then
        ArgumentCaptor<NotificationContext> contextCaptor = ArgumentCaptor.forClass(NotificationContext.class);
        verify(slackNotificationService).sendNotification(contextCaptor.capture());

        NotificationContext context = contextCaptor.getValue();
        assertThat(context.type()).isEqualTo(NotificationContext.NotificationType.ERROR);
        assertThat(context.details()).containsEntry("상태", "FAILED");
        assertThat(context.details()).containsEntry("에러메시지", "Network Timeout");
    }
}
