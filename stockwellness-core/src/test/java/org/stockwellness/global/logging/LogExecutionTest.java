package org.stockwellness.global.logging;

import org.junit.jupiter.api.Test;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import static org.assertj.core.api.Assertions.assertThat;

class LogExecutionTest {

    @LogExecution
    static class AnnotatedClass {
        @LogExecution
        public void annotatedMethod() {}
    }

    @Test
    void testAnnotationRetention() {
        Retention retention = LogExecution.class.getAnnotation(Retention.class);
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void testAnnotationTarget() {
        Target target = LogExecution.class.getAnnotation(Target.class);
        assertThat(target.value()).contains(ElementType.TYPE, ElementType.METHOD);
    }

    @Test
    void testAnnotationOnClassAndMethod() throws NoSuchMethodException {
        assertThat(AnnotatedClass.class.isAnnotationPresent(LogExecution.class)).isTrue();
        assertThat(AnnotatedClass.class.getMethod("annotatedMethod").isAnnotationPresent(LogExecution.class)).isTrue();
    }
}
