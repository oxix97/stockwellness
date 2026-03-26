package org.stockwellness.adapter.out.external.slack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SlackNotificationAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    private SlackNotificationAdapter slackNotificationAdapter;

    @BeforeEach
    void setUp() {
        slackNotificationAdapter = new SlackNotificationAdapter(restTemplate, "http://hooks.slack.com/services/test");
    }

    @Test
    @DisplayName("Slack Webhook으로 알림 메시지를 전송한다")
    void sendNotification() {
        // given
        String title = "Batch Failed";
        String content = "Stock price sync job failed for ID: STK001";

        // when
        slackNotificationAdapter.send(title, content);

        // then
        verify(restTemplate).postForEntity(anyString(), any(), eq(String.class));
    }
}
