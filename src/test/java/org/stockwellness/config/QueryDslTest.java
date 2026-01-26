package org.stockwellness.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class QueryDslTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    JPAQueryFactory queryFactory;

    @Test
    @DisplayName("JPAQueryFactory Bean Injection Verification")
    void verifyBeanConfiguration() {
        // Goal: Verify that the QueryDSL configuration is valid and the bean is registered.
        assertThat(context.getBean(JPAQueryFactory.class)).isNotNull();
        assertThat(queryFactory).isNotNull();
    }
}
