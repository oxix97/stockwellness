package org.stockwellness.adapter.out.external.slack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlackNotificationAdapterTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private SlackNotificationAdapter slackNotificationAdapter;

    @BeforeEach
    void setUp() {
        lenient().when(restClientBuilder.build()).thenReturn(restClient);
        
        lenient().when(restClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.body(any())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        
        slackNotificationAdapter = new SlackNotificationAdapter(restClientBuilder, "http://hooks.slack.com/services/test");
    }

    @Test
    @DisplayName("Slack Webhook으로 알림 메시지를 전송한다")
    void sendNotification() {
        // given
        String title = "Batch Failed";
        String content = "Stock price sync job failed for ID: STK001";

        // when & then
        assertDoesNotThrow(() -> slackNotificationAdapter.send(title, content));
        verify(restClient, atLeastOnce()).post();
    }
}
