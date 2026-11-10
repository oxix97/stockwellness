package org.stockwellness.adapter.in.web.actual;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.*;

/** Runs the same real API/JPA/security/concurrency suite against migrated PostgreSQL, never H2 fallback. */
@ExtendWith(ActualAccountPostgresTest.DockerAvailable.class)
class ActualAccountPostgresTest extends ActualAccountControllerTest {
    private static PostgreSQLContainer<?> postgres;

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        postgres = new PostgreSQLContainer<>("postgres:15-alpine");
        postgres.start();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.validate-on-migrate", () -> true);
        registry.add("spring.flyway.baseline-on-migrate", () -> false);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @AfterAll static void stopPostgres() { if (postgres != null) postgres.stop(); }

    @Test void actual_migration_constraints_and_simulation_are_independent() {
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where version = '1' and success", Integer.class)).isEqualTo(1);
        jdbc.update("insert into portfolio(member_id,name,created_at) values (7,'simulation sentinel',CURRENT_TIMESTAMP)");
        assertThatThrownBy(() -> jdbc.update("insert into actual_account(member_id,name,base_currency,created_at,updated_at) values (7,'bad','USD',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("insert into actual_account(member_id,name,base_currency,created_at,updated_at) values (999999,'bad','KRW',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("select count(*) from portfolio where name='simulation sentinel'", Integer.class)).isEqualTo(1);
        jdbc.update("delete from portfolio where name='simulation sentinel'");
    }

    static class DockerAvailable implements ExecutionCondition {
        @Override public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            return DockerClientFactory.instance().isDockerAvailable()
                    ? ConditionEvaluationResult.enabled("PostgreSQL Testcontainers available")
                    : ConditionEvaluationResult.disabled("PostgreSQL unverified: Docker daemon unavailable; no H2 fallback");
        }
    }
}
