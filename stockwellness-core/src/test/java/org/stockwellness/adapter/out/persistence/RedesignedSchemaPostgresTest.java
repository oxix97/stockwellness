package org.stockwellness.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.stockwellness.domain.portfolio.advisor.AdviceAction;
import org.stockwellness.domain.portfolio.advisor.AdvisorReport;
import org.stockwellness.support.ManualSqlTestSupport;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class RedesignedSchemaPostgresTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbc;

    @BeforeAll
    static void createSchema() throws Exception {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        ManualSqlTestSupport.initializeRedesignedPostgres(dataSource);
    }

    @Test
    void createsExactlyThirtySevenTablesAndConfirmedMarketSeeds() {
        assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_schema='public' and table_type='BASE TABLE'", Integer.class))
                .isEqualTo(37);
        assertThat(jdbc.queryForObject("select count(*) from market", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("select to_regclass('public.flyway_schema_history') is null", Boolean.class)).isTrue();
    }

    @Test
    void validates_all_runtime_entities_against_the_redesigned_schema() {
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        factory.setPackagesToScan("org.stockwellness");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(java.util.Map.of(
                "hibernate.hbm2ddl.auto", "validate",
                "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));

        try {
            factory.afterPropertiesSet();
        } finally {
            factory.destroy();
        }
    }

    @Test
    void persistsAdvisorReportThroughItsCanonicalJsonbMapping() {
        long memberId = member("advisor-report");
        long portfolioId = jdbc.queryForObject(
                "insert into portfolio(member_id,name) values (?, 'advisor test') returning id",
                Long.class, memberId);

        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        factory.setPackagesToScan("org.stockwellness.domain.portfolio.advisor");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(java.util.Map.of(
                "hibernate.hbm2ddl.auto", "validate",
                "hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));

        EntityManagerFactory entityManagerFactory = null;
        Long reportId;
        try {
            factory.afterPropertiesSet();
            entityManagerFactory = factory.getObject();
            var entityManager = entityManagerFactory.createEntityManager();
            entityManager.getTransaction().begin();
            var report = AdvisorReport.create(portfolioId, "gpt-4o-mini", "unversioned", "abc123",
                    "조언", AdviceAction.REBALANCE);
            entityManager.persist(report);
            entityManager.getTransaction().commit();
            reportId = report.getId();
            entityManager.close();

            var reader = entityManagerFactory.createEntityManager();
            var loaded = reader.find(AdvisorReport.class, reportId);
            assertThat(loaded.getContent()).isEqualTo("조언");
            assertThat(loaded.getAction()).isEqualTo(AdviceAction.REBALANCE);
            assertThat(loaded.getInputManifest()).containsEntry("context_sha256", "abc123");
            assertThat(loaded.getGeneratedAt()).isNotNull();
            reader.close();
        } finally {
            if (entityManagerFactory != null) factory.destroy();
        }

        assertThat(jdbc.queryForObject("select result->>'content' from portfolio_ai_report where id=?", String.class, reportId))
                .isEqualTo("조언");
    }

    @Test
    void outbox_schema_matches_the_existing_jpa_entity_contract() {
        assertThat(jdbc.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema='public' and table_name='outbox_event'
                  and ((column_name='payload' and data_type='text')
                    or (column_name='status' and data_type='character varying')
                    or (column_name='processed_at' and data_type='timestamp without time zone')
                    or (column_name='error_message' and data_type='text')
                    or (column_name='created_at' and data_type='timestamp without time zone')
                    or (column_name='updated_at' and data_type='timestamp without time zone'))
                """, Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("""
                select pg_get_constraintdef(oid) from pg_constraint
                where conrelid = 'outbox_event'::regclass and contype='c'
                """, String.class)).contains("COMPLETED");
    }

    @Test
    void createsIndexesForForeignKeyLookupsThatHaveNoLeadingPrimaryIndex() {
        assertThat(jdbc.queryForObject("""
                select count(*) from pg_indexes where schemaname='public' and indexname in (
                    'idx_stock_sector', 'idx_index_series_index', 'idx_index_series_calendar_market',
                    'idx_index_constituent_stock', 'idx_dividend_stock_ex_date', 'idx_market_signal_stock',
                    'idx_stock_ai_signal', 'idx_sector_leading_stock_stock', 'idx_member_identity_member',
                    'idx_portfolio_item_stock')
                """, Integer.class)).isEqualTo(10);
    }

    @Test
    void enforcesNaturalAndPartialUniqueKeysCompositeOwnershipAndDateExclusion() {
        assertThatThrownBy(() -> jdbc.update("insert into market(code,name,country_code,currency,timezone) values ('KOSPI','dup','KR','KRW','Asia/Seoul')"))
                .isInstanceOf(DataAccessException.class);

        long memberA = member("alpha");
        long memberB = member("beta");
        long accountB = account(memberB);
        long group = jdbc.queryForObject("insert into watchlist_group(member_id,name) values (?, 'watch') returning id", Long.class, memberA);
        long stock = stock("005930");
        jdbc.update("insert into watchlist_item(watchlist_group_id,stock_id) values (?,?)", group, stock);
        assertThatThrownBy(() -> jdbc.update("insert into watchlist_item(watchlist_group_id,stock_id) values (?,?)", group, stock))
                .isInstanceOf(DataAccessException.class);
        jdbc.update("update watchlist_item set deleted_at=now() where watchlist_group_id=? and stock_id=?", group, stock);
        jdbc.update("insert into watchlist_item(watchlist_group_id,stock_id) values (?,?)", group, stock);

        assertThatThrownBy(() -> jdbc.update("""
                insert into actual_mutation_receipt(
                    member_id,account_scope,response_account_id,operation,request_key,request_fingerprint,response_body)
                values (?,0,?,'CREATE','wrong-owner','fingerprint','{}')
                """, memberA, accountB)).isInstanceOf(DataAccessException.class);

        long index = jdbc.queryForObject("insert into market_index(index_key,name,kind) values ('TEST','test','SECTOR') returning id", Long.class);
        jdbc.update("insert into index_constituent(index_id,stock_id,valid_from) values (?,?,date '2026-01-01')", index, stock);
        assertThatThrownBy(() -> jdbc.update("insert into index_constituent(index_id,stock_id,valid_from) values (?,?,date '2026-06-01')", index, stock))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsIncompleteFinalPricesAndNumericOverflowAndCreatesBatchSequences() {
        long stock = stock("005931");
        assertThatThrownBy(() -> jdbc.update("""
                insert into stock_price_daily(stock_id,trade_date,quote_scope,data_status,source,collected_at)
                values (?,date '2026-10-02','UNIFIED','FINAL','test',now())
                """, stock)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("""
                insert into stock_price_daily(stock_id,trade_date,quote_scope,close_price,data_status,source,collected_at)
                values (?,date '2026-10-02','UNIFIED',1234567890123456,'PROVISIONAL','test',now())
                """, stock)).isInstanceOf(DataAccessException.class);
        assertThat(jdbc.queryForObject("select nextval('batch_job_seq')", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select nextval('batch_job_execution_seq')", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select nextval('batch_step_execution_seq')", Long.class)).isEqualTo(1L);
    }

    @Test
    void distinguishesProviderNamespacesAndReturnSeriesAndRejectsDuplicateCanonicalSeries() {
        jdbc.update("insert into stock(market_code,ticker,name,market_type,currency,status) values ('KOSPI','DUP001','A','KOSPI','KRW','ACTIVE')");
        jdbc.update("insert into stock(market_code,ticker,name,market_type,currency,status) values ('KOSDAQ','DUP001','B','KOSDAQ','KRW','ACTIVE')");
        assertThatThrownBy(() -> jdbc.update("insert into stock(market_code,ticker,name,market_type,currency,status) values ('KOSPI','DUP001','C','KOSPI','KRW','ACTIVE')"))
                .isInstanceOf(DataAccessException.class);

        long kospi = jdbc.queryForObject("insert into market_index(index_key,name,kind,public_benchmark_code) values ('KRX:KOSPI:0001','KOSPI','MARKET','0001') returning id", Long.class);
        long kosdaq = jdbc.queryForObject("insert into market_index(index_key,name,kind,public_benchmark_code) values ('KRX:KOSDAQ:1001','KOSDAQ','MARKET','1001') returning id", Long.class);

        jdbc.update("insert into index_series(index_id,provider,provider_namespace,provider_series_code,calendar_market_code,return_basis,dividend_basis,currency,is_canonical) values (?, 'KIS','kis.idxcode.KOSPI','0001','KOSPI','PRICE','NONE','KRW',true)", kospi);
        jdbc.update("insert into index_series(index_id,provider,provider_namespace,provider_series_code,calendar_market_code,return_basis,dividend_basis,currency,is_canonical) values (?, 'KIS','kis.idxcode.KOSDAQ','0001','KOSDAQ','PRICE','NONE','KRW',true)", kosdaq);
        jdbc.update("insert into index_series(index_id,provider,provider_namespace,provider_series_code,calendar_market_code,return_basis,dividend_basis,currency,is_canonical) values (?, 'KIS','kis.tr','0001.TR','KOSPI','TOTAL_RETURN','GROSS','KRW',true)", kospi);
        jdbc.update("insert into index_series(index_id,provider,provider_namespace,provider_series_code,calendar_market_code,return_basis,dividend_basis,currency,is_active,is_canonical) values (?, 'OTHER','disabled','KOSPI','KOSPI','PRICE','NONE','KRW',false,true)", kospi);

        assertThatThrownBy(() -> jdbc.update("insert into index_series(index_id,provider,provider_namespace,provider_series_code,calendar_market_code,return_basis,dividend_basis,currency,is_canonical) values (?, 'OTHER','alt','KOSPI','KOSPI','PRICE','NONE','KRW',true)", kospi))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("insert into index_series(index_id,provider,provider_namespace,provider_series_code,calendar_market_code,return_basis,dividend_basis,currency,is_canonical) values (?, 'KIS','bad.tr','0001.TR','KOSPI','TOTAL_RETURN','NONE','KRW',false)", kospi))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbc.queryForObject("select count(*) from index_series where provider='KIS' and provider_series_code='0001'", Integer.class))
                .isEqualTo(2);
    }

    private long member(String nickname) {
        return jdbc.queryForObject("insert into member(nickname,role) values (?, 'USER') returning id", Long.class, nickname);
    }

    private long account(long memberId) {
        return jdbc.queryForObject("insert into actual_account(member_id,name,base_currency) values (?,'test','KRW') returning id", Long.class, memberId);
    }

    private long stock(String ticker) {
        return jdbc.queryForObject("insert into stock(market_code,ticker,name,market_type,currency,status) values ('KOSPI',?,'test','KOSPI','KRW','ACTIVE') returning id", Long.class, ticker);
    }
}
