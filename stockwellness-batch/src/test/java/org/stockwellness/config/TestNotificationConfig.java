package org.stockwellness.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import org.stockwellness.application.port.out.notification.NotificationPort;

@TestConfiguration
public class TestNotificationConfig {

    @Bean
    @Primary
    public NotificationPort notificationPort() {
        return Mockito.mock(NotificationPort.class);
    }

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return Mockito.mock(RestTemplate.class);
    }
}
