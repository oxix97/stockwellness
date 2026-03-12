package org.stockwellness.global.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.stockwellness.application.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = LoggingAspectContentTest.TestConfig.class)
class LoggingAspectContentTest {

    @Autowired
    private TestService testService;

    private ListAppender<ILoggingEvent> listAppender;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    void testLoggingContent() throws IOException {
        testService.execute();

        List<ILoggingEvent> logs = listAppender.list;
        assertThat(logs).isNotEmpty();

        String logMessage = logs.get(0).getFormattedMessage();
        JsonNode jsonNode = objectMapper.readTree(logMessage);

        assertThat(jsonNode.has("methodName")).isTrue();
        assertThat(jsonNode.get("methodName").asText()).isEqualTo("execute");
        assertThat(jsonNode.has("className")).isTrue();
        assertThat(jsonNode.has("executionTimeMs")).isTrue();
    }

    @Test
    void testExceptionLogging() throws IOException {
        assertThrows(RuntimeException.class, () -> testService.throwException());

        List<ILoggingEvent> logs = listAppender.list;
        assertThat(logs).isNotEmpty();

        String logMessage = logs.get(logs.size() - 1).getFormattedMessage();
        JsonNode jsonNode = objectMapper.readTree(logMessage);

        assertThat(jsonNode.get("methodName").asText()).isEqualTo("throwException");
        assertThat(jsonNode.has("exceptionMessage")).isTrue();
        assertThat(jsonNode.get("exceptionMessage").asText()).isEqualTo("Test Exception");
    }

    @Test
    void testArgsAndResultLogging() throws IOException {
        String result = testService.executeWithArgs("Hello", 123);
        assertThat(result).isEqualTo("Hello 123");

        List<ILoggingEvent> logs = listAppender.list;
        assertThat(logs).isNotEmpty();

        String logMessage = logs.get(logs.size() - 1).getFormattedMessage();
        JsonNode jsonNode = objectMapper.readTree(logMessage);

        assertThat(jsonNode.get("methodName").asText()).isEqualTo("executeWithArgs");
        assertThat(jsonNode.has("args")).isTrue();
        assertThat(jsonNode.get("args").isArray()).isTrue();
        assertThat(jsonNode.get("args").get(0).asText()).isEqualTo("Hello");
        assertThat(jsonNode.get("args").get(1).asInt()).isEqualTo(123);
        assertThat(jsonNode.get("result").asText()).isEqualTo("Hello 123");
    }

    @Configuration
    @EnableAspectJAutoProxy
    @Import(LoggingAspect.class)
    static class TestConfig {
        @Bean
        public TestService testService() {
            return new TestService();
        }
    }
}
