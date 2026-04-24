package org.stockwellness.global.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = LoggingAspectReentrancyTest.TestConfig.class)
class LoggingAspectReentrancyTest {

    @Autowired
    private NestedController nestedController;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingAspect.class);
        logger.detachAppender(listAppender);
    }

    @Test
    void testReentrancyProtection() {
        // When: Controller -> Service -> Adapter 순으로 호출
        nestedController.callService();

        // Then: 재진입 방지가 동작한다면 로그는 가장 바깥쪽 호출(Controller)에 대해 1개만 찍혀야 함
        // 수정 전이라면 3개가 찍혔을 것임
        assertThat(listAppender.list).hasSize(1);
        
        ILoggingEvent logEvent = listAppender.list.get(0);
        assertThat(logEvent.getFormattedMessage()).contains("\"className\":\"NestedController\"");
        assertThat(logEvent.getFormattedMessage()).contains("\"methodName\":\"callService\"");
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @Import(LoggingAspect.class)
    static class TestConfig {
        @Bean
        public NestedController nestedController(NestedService nestedService) {
            return new NestedController(nestedService);
        }

        @Bean
        public NestedService nestedService(NestedAdapter nestedAdapter) {
            return new NestedService(nestedAdapter);
        }

        @Bean
        public NestedAdapter nestedAdapter() {
            return new NestedAdapter();
        }
    }

    @Component
    static class NestedController {
        private final NestedService service;
        public NestedController(NestedService service) { this.service = service; }
        public void callService() { service.callAdapter(); }
    }

    @Component
    static class NestedService {
        private final NestedAdapter adapter;
        public NestedService(NestedAdapter adapter) { this.adapter = adapter; }
        public void callAdapter() { adapter.execute(); }
    }

    @Component
    static class NestedAdapter {
        public void execute() {}
    }
}
