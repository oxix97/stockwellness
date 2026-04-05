package org.stockwellness.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.stockwellness.adapter.out.external.kis.client.KisMasterClient;
import org.stockwellness.application.port.out.notification.NotificationPort;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BatchIntegrationTestSupport extends InfrastructureTestSupport {

    @MockitoBean
    protected NotificationPort notificationPort;

    @MockitoBean
    protected RestTemplate restTemplate;

    @MockitoBean
    protected KisMasterClient kisMasterClient;

    @MockitoBean
    protected org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    protected org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory redisConnectionFactory;
}
