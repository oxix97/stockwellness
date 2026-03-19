package org.stockwellness.global.logging;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = LoggingAspectTest.TestConfig.class)
class LoggingAspectTest {

    @Autowired
    private AnnotatedService annotatedService;

    @Autowired
    private MockService mockService;

    @Autowired
    private MockController mockController;

    @Autowired
    private MockAdapterOut mockAdapterOut;

    @Autowired
    private NonTargetClass nonTargetClass;

    @Test
    void testPointcutMatching() {
        assertThat(AopUtils.isAopProxy(annotatedService)).as("AnnotatedService should be a proxy").isTrue();
        assertThat(AopUtils.isAopProxy(mockService)).as("MockService should be a proxy").isTrue();
        assertThat(AopUtils.isAopProxy(mockController)).as("MockController should be a proxy").isTrue();
        assertThat(AopUtils.isAopProxy(mockAdapterOut)).as("MockAdapterOut should be a proxy").isTrue();
        assertThat(AopUtils.isAopProxy(nonTargetClass)).as("NonTargetClass should NOT be a proxy").isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testMaskingLogic() throws Exception {
        // Given
        TestDto dto = TestDto.builder()
                .username("user1")
                .password("secret123")
                .email("user1@example.com")
                .accessToken("tk_12345")
                .nested(TestDto.NestedDto.builder()
                        .secretValue("hidden")
                        .publicValue("visible")
                        .build())
                .build();

        // When
        com.fasterxml.jackson.databind.ObjectMapper mapper = MaskingObjectMapper.create();
        String json = mapper.writeValueAsString(dto);
        Map<String, Object> resultMap = mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});

        // Then
        assertThat(resultMap.get("username")).isEqualTo("user1");
        assertThat(resultMap.get("password")).isEqualTo("********");
        assertThat(resultMap.get("email")).isEqualTo("********");
        assertThat(resultMap.get("accessToken")).isEqualTo("********");

        Map<String, Object> nestedMap = (Map<String, Object>) resultMap.get("nested");
        assertThat(nestedMap.get("secretValue")).isEqualTo("********");
        assertThat(nestedMap.get("publicValue")).isEqualTo("visible");
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @Import(LoggingAspect.class)
    static class TestConfig {
        @Bean
        public AnnotatedService annotatedService() {
            return new AnnotatedService();
        }

        @Bean
        public MockService mockService() {
            return new MockService();
        }

        @Bean
        public MockController mockController() {
            return new MockController();
        }

        @Bean
        public MockAdapterOut mockAdapterOut() {
            return new MockAdapterOut();
        }

        @Bean
        public NonTargetClass nonTargetClass() {
            return new NonTargetClass();
        }
    }

    @LogExecution
    static class AnnotatedService {
        public void execute() {}
    }

    @Component
    static class MockService {
        public void execute() {}
    }

    @Component
    static class MockController {
        public void execute() {}
    }

    @Component
    static class MockAdapterOut {
        public void execute() {}
    }

    @Component
    static class NonTargetClass {
        public void execute() {}
    }
}
