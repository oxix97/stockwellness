package org.stockwellness.support.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.security.test.context.support.WithSecurityContext;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = MockMemberSecurityContextFactory.class)
public @interface MockMember {
    long id() default 1L;
    String email() default "test@example.com";
    String nickname() default "tester";
    String role() default "USER";
}
