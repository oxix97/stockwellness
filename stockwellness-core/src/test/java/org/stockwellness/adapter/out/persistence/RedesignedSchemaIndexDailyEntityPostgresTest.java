package org.stockwellness.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Properties;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.stockwellness.domain.stock.insight.BenchmarkDataStatus;
import org.stockwellness.domain.stock.insight.IndexDaily;
import org.stockwellness.domain.stock.insight.IndexSeries;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.support.ManualSqlTestSupport;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class RedesignedSchemaIndexDailyEntityPostgresTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 10, 2);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Test
    void indexDailyEntityUsesSeriesKeyAndTargetSchemaColumns() {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        ManualSqlTestSupport.initializeRedesignedPostgres(dataSource);
        Properties settings = new Properties();
        settings.setProperty("hibernate.connection.url", POSTGRES.getJdbcUrl());
        settings.setProperty("hibernate.connection.username", POSTGRES.getUsername());
        settings.setProperty("hibernate.connection.password", POSTGRES.getPassword());
        settings.setProperty("hibernate.connection.driver_class", POSTGRES.getDriverClassName());
        settings.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        settings.setProperty("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        settings.setProperty("hibernate.hbm2ddl.auto", "validate");

        try (SessionFactory sessions = new Configuration()
                .addAnnotatedClass(MarketIndex.class)
                .addAnnotatedClass(IndexSeries.class)
                .addAnnotatedClass(IndexDaily.class)
                .addProperties(settings)
                .buildSessionFactory()) {
            Long seriesId;
            try (var session = sessions.openSession()) {
                var transaction = session.beginTransaction();
                MarketIndex index = MarketIndex.of("0001", "코스피", "0");
                session.persist(index);
                session.flush();
                IndexSeries series = new IndexSeries(index, "KIS", "kis.idxcode.KOSPI", "0001",
                        "KOSPI", "PRICE", "NONE", "KRW", true);
                session.persist(series);
                session.flush();
                seriesId = series.getId();
                session.persist(IndexDaily.of(series, TRADE_DATE,
                        new BigDecimal("2490"), new BigDecimal("2510"), new BigDecimal("2480"), new BigDecimal("2500"),
                        100L, new BigDecimal("100000"), null, null, null, null, null, null, null,
                        "FHKUP03500100", null, BenchmarkDataStatus.PROVISIONAL, null));
                transaction.commit();
            }

            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            assertThat(jdbc.queryForObject("select count(*) from index_daily where series_id = ? and trade_date = ?",
                    Integer.class, seriesId, TRADE_DATE)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select close_value from index_daily where series_id = ? and trade_date = ?",
                    BigDecimal.class, seriesId, TRADE_DATE)).isEqualByComparingTo("2500");
            assertThat(jdbc.queryForObject("select revision from index_daily where series_id = ? and trade_date = ?",
                    Integer.class, seriesId, TRADE_DATE)).isEqualTo(1);
            assertThatThrownBy(() -> jdbc.update("""
                    insert into index_daily(series_id, trade_date, low_value, price_status, price_source, collected_at)
                    values (?, ?, -1, 'PROVISIONAL', 'test', now())
                    """, seriesId, TRADE_DATE.plusDays(1))).isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> jdbc.update("""
                    insert into index_daily(series_id, trade_date, price_status, price_source, collected_at,
                                            supply_status, supply_source, supply_collected_at)
                    values (?, ?, 'PROVISIONAL', 'test', now(), 'PROVISIONAL', 'test', now())
                    """, seriesId, TRADE_DATE.plusDays(2))).isInstanceOf(DataAccessException.class);
        }
    }
}
