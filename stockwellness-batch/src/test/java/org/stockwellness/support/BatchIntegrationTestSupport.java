package org.stockwellness.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.stockwellness.adapter.out.external.kis.client.KisMasterClient;
import org.stockwellness.application.port.out.notification.NotificationPort;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BatchIntegrationTestSupport {

    @MockBean
    protected NotificationPort notificationPort;

    @MockBean
    protected RestTemplate restTemplate;

    @MockBean
    protected KisMasterClient kisMasterClient;
}
