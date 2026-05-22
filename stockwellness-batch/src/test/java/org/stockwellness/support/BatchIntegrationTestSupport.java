package org.stockwellness.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;
import org.stockwellness.adapter.out.external.kis.client.KisMasterClient;
import org.stockwellness.application.port.out.notification.NotificationPort;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BatchIntegrationTestSupport extends InfrastructureTestSupport {

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @MockitoBean
    protected NotificationPort notificationPort;

    @MockitoBean
    protected RestTemplate restTemplate;

    @MockitoBean
    protected KisMasterClient kisMasterClient;

    @MockitoBean
    protected KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    protected LettuceConnectionFactory redisConnectionFactory;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.execute();
    }
}
