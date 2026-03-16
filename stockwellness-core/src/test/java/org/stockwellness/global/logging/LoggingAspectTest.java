package org.stockwellness.global.logging;

import org.junit.jupiter.api.Test;
import org.stockwellness.adapter.in.web.TestController;
import org.stockwellness.adapter.out.persistence.TestAdapterOut;
import org.stockwellness.application.service.TestService;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = LoggingAspectTest.TestConfig.class)
class LoggingAspectTest {

    @Autowired
    private LoggingAspect loggingAspect;

    @Autowired
    private AnnotatedService annotatedService;

    @Autowired
    private TestService testService;

    @Autowired
    private TestController testController;

    @Autowired
    private TestAdapterOut testAdapterOut;

    @Autowired
    private NonTargetClass nonTargetClass;

    @Test
    void testPointcutMatching() {
        assertThat(AopUtils.isAopProxy(annotatedService)).as("AnnotatedService should be a proxy").isTrue();
        assertThat(AopUtils.isAopProxy(testService)).as("TestService should be a proxy").isTrue();
        assertThat(AopUtils.isAopProxy(testController)).as("TestController should be a proxy").isTrue();
        assertThat(AopUtils.isAopProxy(testAdapterOut)).as("TestAdapterOut should be a proxy").isTrue();
        assertThat(AopUtils.isAopProxy(nonTargetClass)).as("NonTargetClass should NOT be a proxy").isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testMaskingLogic() {
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
        Object maskedResult = ReflectionTestUtils.invokeMethod(loggingAspect, "maskSensitiveData", (Object) new Object[]{dto});

        // Then
        assertThat(maskedResult).isInstanceOf(java.util.List.class);
        java.util.List<?> results = (java.util.List<?>) maskedResult;
        Map<String, Object> resultMap = (Map<String, Object>) results.get(0);

        assertThat(resultMap.get("username")).isEqualTo("user1");
        assertThat(resultMap.get("password")).isEqualTo("********");
        assertThat(resultMap.get("email")).isEqualTo("********");
        assertThat(resultMap.get("accessToken")).isEqualTo("********");

        Map<String, Object> nestedMap = (Map<String, Object>) resultMap.get("nested");
        assertThat(nestedMap.get("secretValue")).isEqualTo("********");
        assertThat(nestedMap.get("publicValue")).isEqualTo("visible");
    }

    @Configuration
    @EnableAspectJAutoProxy
    @Import(LoggingAspect.class)
    static class TestConfig {
        @Bean
        public AnnotatedService annotatedService() {
            return new AnnotatedService();
        }

        @Bean
        public TestService testService() {
            return new TestService();
        }

        @Bean
        public TestController testController() {
            return new TestController();
        }

        @Bean
        public TestAdapterOut testAdapterOut() {
            return new TestAdapterOut();
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
    static class NonTargetClass {
        public void execute() {}
    }
}
