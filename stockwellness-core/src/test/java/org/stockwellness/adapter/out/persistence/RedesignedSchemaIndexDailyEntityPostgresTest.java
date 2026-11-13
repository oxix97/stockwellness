package org.stockwellness.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Properties;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.stockwellness.adapter.out.persistence.stock.EodStockPriceAdapter;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.Market;
import org.stockwellness.domain.stock.TradingCalendar;
import org.stockwellness.domain.stock.insight.BenchmarkDataStatus;
import org.stockwellness.domain.stock.insight.IndexDaily;
import org.stockwellness.domain.stock.insight.IndexSeries;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.price.EodStockPrice;
import org.stockwellness.domain.stock.price.StockPriceAdjustment;
import org.stockwellness.support.ManualSqlTestSupport;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class RedesignedSchemaIndexDailyEntityPostgresTest {
    private static final LocalDate TRADE_DATE = LocalDate.of(2026, 10, 2);
    private static final LocalDate CALENDAR_DATE = LocalDate.of(2026, 10, 5);

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
                .addAnnotatedClass(Market.class)
                .addAnnotatedClass(TradingCalendar.class)
                .addAnnotatedClass(MarketIndex.class)
                .addAnnotatedClass(IndexSeries.class)
                .addAnnotatedClass(IndexDaily.class)
                .addProperties(settings)
                .buildSessionFactory()) {
            Long seriesId;
            try (var session = sessions.openSession()) {
                var transaction = session.beginTransaction();
                assertThat(session.find(Market.class, "KOSPI")).isNotNull();
                session.persist(TradingCalendar.of("KOSPI", CALENDAR_DATE, true,
                        OffsetDateTime.of(2026, 10, 5, 0, 0, 0, 0, ZoneOffset.UTC),
                        OffsetDateTime.of(2026, 10, 5, 6, 30, 0, 0, ZoneOffset.UTC), "test"));
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
                        100L, new BigDecimal("100000"), null, null, null, null, null,
                        new BigDecimal("-100"), BigDecimal.ZERO, "FHKUP03500100", "KIS:TEST:SUPPLY",
                        BenchmarkDataStatus.PROVISIONAL, BenchmarkDataStatus.PROVISIONAL));
                transaction.commit();
            }

            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            assertThat(jdbc.queryForObject("select is_trading_day from trading_calendar where market_code='KOSPI' and trade_date=?",
                    Boolean.class, CALENDAR_DATE)).isTrue();
            assertThat(jdbc.queryForObject("select opens_at from trading_calendar where market_code='KOSPI' and trade_date=?",
                    OffsetDateTime.class, CALENDAR_DATE)).isEqualTo(OffsetDateTime.of(2026, 10, 5, 0, 0, 0, 0, ZoneOffset.UTC));
            assertThat(jdbc.queryForObject("select closes_at from trading_calendar where market_code='KOSPI' and trade_date=?",
                    OffsetDateTime.class, CALENDAR_DATE)).isEqualTo(OffsetDateTime.of(2026, 10, 5, 6, 30, 0, 0, ZoneOffset.UTC));
            assertThatThrownBy(() -> jdbc.update("""
                    insert into trading_calendar(market_code, trade_date, is_trading_day, source, collected_at)
                    values ('KOSPI', ?, true, 'test', now())
                    """, CALENDAR_DATE.plusDays(1))).isInstanceOf(DataAccessException.class);
            long stockId = jdbc.queryForObject("""
                    insert into stock(market_code, ticker, name, currency, status)
                    values ('KOSPI', 'TEST01', '테스트', 'KRW', 'ACTIVE') returning id
                    """, Long.class);
            Stock stock = mock(Stock.class);
            when(stock.getId()).thenReturn(stockId);
            EodStockPriceAdapter prices = new EodStockPriceAdapter(jdbc);
            prices.upsert(List.of(EodStockPrice.of(stock, TRADE_DATE, "KRX",
                    new BigDecimal("2490"), new BigDecimal("2510"), new BigDecimal("2480"),
                    new BigDecimal("2500"), 100L, new BigDecimal("100000"), "KIS:RAW", "PROVISIONAL")));
            prices.upsertAdjustments(List.of(StockPriceAdjustment.of(stock, TRADE_DATE, "KRX",
                    TRADE_DATE.plusDays(1), new BigDecimal("2490"), new BigDecimal("2510"),
                    new BigDecimal("2480"), new BigDecimal("2500"), "KIS:ADJUSTED")));
            assertThat(jdbc.queryForObject("select close_price from stock_price_daily where stock_id=?",
                    BigDecimal.class, stockId)).isEqualByComparingTo("2500");
            assertThat(jdbc.queryForObject("select adjusted_close from stock_price_adjustment where stock_id=?",
                    BigDecimal.class, stockId)).isEqualByComparingTo("2500");
            assertThat(jdbc.queryForObject("select count(*) from index_daily where series_id = ? and trade_date = ?",
                    Integer.class, seriesId, TRADE_DATE)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select close_value from index_daily where series_id = ? and trade_date = ?",
                    BigDecimal.class, seriesId, TRADE_DATE)).isEqualByComparingTo("2500");
            assertThat(jdbc.queryForObject("select revision from index_daily where series_id = ? and trade_date = ?",
                    Integer.class, seriesId, TRADE_DATE)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select foreign_net_amount from index_daily where series_id = ? and trade_date = ?",
                    BigDecimal.class, seriesId, TRADE_DATE)).isEqualByComparingTo("-100");
            assertThat(jdbc.queryForObject("select supply_revision from index_daily where series_id = ? and trade_date = ?",
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
