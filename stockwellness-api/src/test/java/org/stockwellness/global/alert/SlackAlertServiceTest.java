package org.stockwellness.global.alert;

import java.net.URI;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SlackAlertServiceTest {

    @Test
    @DisplayName("webhook URL이 비어있으면 RestClient를 호출하지 않는다")
    void noCallWhenWebhookUrlIsBlank() {
        RestClient restClient = mock(RestClient.class);
        SlackAlertService service = new SlackAlertService(new SlackAlertProperties(""), restClient);

        service.sendInternalServerErrorAlert("abc123", new RuntimeException("test error"));

        verify(restClient, never()).post();
    }

    @Test
    @DisplayName("webhook URL이 설정되면 Slack으로 메시지를 전송한다")
    void sendMessageWhenWebhookUrlIsSet() {
        RestClient restClient = mock(RestClient.class, Answers.RETURNS_DEEP_STUBS);

        SlackAlertService service = new SlackAlertService(
                new SlackAlertProperties("https://hooks.slack.com/test"),
                restClient
        );
        service.sendInternalServerErrorAlert("abc123", new RuntimeException("test error"));

        verify(restClient).post();
    }

    @Test
    @DisplayName("Slack 전송 실패 시 예외가 외부로 전파되지 않는다")
    void noExceptionPropagationOnSendFailure() {
        RestClient restClient = mock(RestClient.class, Answers.RETURNS_DEEP_STUBS);
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        Mockito.when(restClient.post()).thenReturn(uriSpec);
        Mockito.when(uriSpec.uri(ArgumentMatchers.any(URI.class)))
                .thenThrow(new RuntimeException("Slack 연결 실패"));

        SlackAlertService service = new SlackAlertService(
                new SlackAlertProperties("https://hooks.slack.com/test"),
                restClient
        );

        assertThatNoException().isThrownBy(() ->
                service.sendInternalServerErrorAlert("abc123", new RuntimeException("original error"))
        );
    }
}
