package org.stockwellness.adapter.out.persistence.stock;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class ExistingDatabaseMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void upgradesVersion19AndPreservesAllOriginalSectorCodes() {
        var source = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var jdbc = new JdbcTemplate(source);
        Flyway.configure().dataSource(source).target("19").load().migrate();
        jdbc.update("""
                INSERT INTO stock(currency, is_preferred, is_premium_tracking, created_at, market_type,
                                  status, ticker, name, sector_large_code, sector_medium_code, sector_small_code)
                VALUES ('KRW', false, false, CURRENT_TIMESTAMP, 'KOSPI', 'ACTIVE', 'TEST', 'Test', '1', '2', '3')
                """);
        jdbc.execute("ALTER TABLE stock ADD COLUMN sector_code varchar(10)");
        jdbc.execute("ALTER TABLE sector_insight ADD COLUMN advance_ratio numeric(10,2)");
        jdbc.execute("CREATE SEQUENCE batch_job_seq START WITH 3000");
        jdbc.update("UPDATE stock SET sector_code = 'EXISTING' WHERE ticker = 'TEST'");
        jdbc.execute("""
                CREATE TABLE sector_weather (
                    id bigserial PRIMARY KEY, base_date date NOT NULL, sector_code varchar(20) NOT NULL,
                    weather_score int NOT NULL, weather_state varchar(20) NOT NULL,
                    ai_title varchar(100), ai_insight text, created_at timestamp NOT NULL, updated_at timestamp
                )
                """);
        jdbc.update("""
                INSERT INTO sector_weather(base_date, sector_code, weather_score, weather_state, created_at)
                VALUES ('2026-01-01', 'S', 10, 'OLD', '2026-01-01'),
                       ('2026-01-01', 'S', 20, 'NEW', '2026-01-02')
                """);
        Flyway.configure().dataSource(source).load().migrate();
        assertThat(jdbc.queryForMap("SELECT sector_code, sector_large_code, sector_medium_code, sector_small_code FROM stock WHERE ticker = 'TEST'"))
                .containsEntry("sector_code", "EXISTING")
                .containsEntry("sector_large_code", "1")
                .containsEntry("sector_medium_code", "2")
                .containsEntry("sector_small_code", "3");
        assertThat(jdbc.queryForObject("SELECT weather_score FROM sector_weather WHERE sector_code = 'S'", Integer.class)).isEqualTo(20);
        assertThat(jdbc.queryForObject("SELECT weather_score FROM sector_weather_duplicate_archive WHERE sector_code = 'S'", Integer.class)).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT nextval('batch_job_seq')", Long.class)).isEqualTo(3000L);
        assertThat(jdbc.queryForObject("SELECT to_regclass('public.actual_account') IS NOT NULL", Boolean.class)).isTrue();
    }
}
