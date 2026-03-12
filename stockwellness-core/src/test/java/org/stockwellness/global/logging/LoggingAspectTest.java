package org.stockwellness.global.logging;

import org.junit.jupiter.api.Test;
import org.stockwellness.adapter.in.web.TestController;
import org.stockwellness.application.service.TestService;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = LoggingAspectTest.TestConfig.class)
class LoggingAspectTest {

    @Autowired
    private AnnotatedService annotatedService;

    @Autowired
    private TestService testService;

    @Autowired
    private TestController testController;

    @Autowired
    private NonTargetClass nonTargetClass;

    @Test
    void testPointcutMatching() {
        assertThat(AopUtils.isAopProxy(annotatedService)).as("AnnotatedService should be a proxy").isTrue();
        assertThat(AopUtils.isAopProxy(testService)).as("TestService should be a proxy").isTrue();
        assertThat(AopUtils.isAopProxy(testController)).as("TestController should be a proxy").isTrue();
        assertThat(AopUtils.isAopProxy(nonTargetClass)).as("NonTargetClass should NOT be a proxy").isFalse();
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
