package org.stockwellness.integration.marketdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyLong;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.errors.TimeoutException;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.stockwellness.adapter.in.kafka.marketdata.MarketDataRevisionCacheConsumer;
import org.stockwellness.adapter.in.web.insight.MarketWeatherController;
import org.stockwellness.adapter.out.persistence.outbox.OutboxAdapter;
import org.stockwellness.adapter.out.persistence.insight.IndexDailyAdapter;
import org.stockwellness.adapter.out.persistence.stock.EodStockPriceAdapter;
import org.stockwellness.adapter.out.persistence.stock.IndexDailyReadAdapter;
import org.stockwellness.adapter.out.persistence.stock.IndexWeatherAdapter;
import org.stockwellness.adapter.out.persistence.stock.StockPriceReadAdapter;
import org.stockwellness.adapter.out.persistence.stock.TechnicalIndicatorAdapter;
import org.stockwellness.application.port.in.batch.MarketDataRevisionUseCase;
import org.stockwellness.application.port.in.batch.MarketDerivedDataUseCase;
import org.stockwellness.application.port.out.outbox.OutboxEventPublisherPort;
import org.stockwellness.application.port.out.outbox.OutboxPort;
import org.stockwellness.application.port.out.stock.IndexDailyPort;
import org.stockwellness.application.port.out.stock.IndexDailyReadPort;
import org.stockwellness.application.port.out.stock.SectorLeadingStockPort;
import org.stockwellness.config.ApiRedisConfig;
import org.stockwellness.config.KafkaTopicConfig;
import org.stockwellness.config.JpaConfig;
import org.stockwellness.config.QueryDslConfig;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.insight.BenchmarkDataStatus;
import org.stockwellness.domain.stock.insight.IndexDaily;
import org.stockwellness.domain.stock.insight.IndexSeries;
import org.stockwellness.domain.stock.event.MarketDataRevisionEvent;
import org.stockwellness.domain.stock.price.EodStockPrice;
import org.stockwellness.application.service.marketdata.MarketDataRevisionService;
import org.stockwellness.application.service.marketdata.MarketDerivedDataService;
import org.stockwellness.application.service.insight.MarketWeatherService;
import org.stockwellness.batch.support.outbox.OutboxRelay;
import org.stockwellness.adapter.in.kafka.marketdata.MarketDataRevisionConsumer;
import org.stockwellness.adapter.out.kafka.outbox.KafkaOutboxEventAdapter;
import org.stockwellness.support.ManualSqlTestSupport;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/** Runs only against explicitly supplied disposable Issue 331 services and synthetic FINAL fixtures. */
@EnabledIfEnvironmentVariable(named = "SW331_ENABLE_INTEGRATION", matches = "true")
@SpringBootTest(
        classes = MarketDataRevisionPropagationPostgresKafkaRedisIntegrationTest.IntegrationApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.jpa.open-in-view=false",
                "spring.sql.init.mode=never",
                "spring.batch.job.enabled=false",
                "spring.batch.jdbc.initialize-schema=never",
                "spring.kafka.listener.auto-startup=true",
                "spring.kafka.producer.acks=all",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "management.health.redis.enabled=false",
                "management.health.kafka.enabled=false"
        })
@ActiveProfiles("test")
class MarketDataRevisionPropagationPostgresKafkaRedisIntegrationTest {
    private static final Duration WAIT = Duration.ofSeconds(45);
    private static final LocalDate FIRST_DATE = LocalDate.of(2026, 1, 1);
    private static final String QUOTE_SCOPE = "KRX";
    private static final String SOURCE = "SYNTHETIC:FINAL:ISSUE-331";
    private static final String DERIVED_GROUP = "issue-331-derived-it";
    private static final String CACHE_GROUP = "issue-331-cache-it";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EodStockPriceAdapter sourceAdapter;
    @Autowired private IndexDailyPort indexDailyPort;
    @Autowired private IndexDailyReadPort indexDailyReadPort;
    @Autowired private MarketDerivedDataService derivedService;
    @Autowired private OutboxRelay relay;
    @Autowired private OutboxPort outboxPort;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired private KafkaListenerEndpointRegistry listenerRegistry;
    @Autowired private CacheManager cacheManager;
    @Autowired private StringRedisTemplate redis;
    @Autowired private Environment environment;
    @Autowired private FaultSwitch faultSwitch;
    @Autowired private CommitGate commitGate;
    @Autowired private RevisionTrace revisionTrace;
    @Autowired private DerivedCallTrace derivedCallTrace;
    @Autowired private FailureGate failureGate;
    @Autowired private CompletionFailureGate completionFailureGate;

    @MockitoBean private SectorLeadingStockPort sectorLeadingStockPort;

    private long stockId;
    private LocalDate targetDate;
    private BigDecimal baselineMa120;
    private String cacheNamespace;

    @DynamicPropertySource
    static void isolatedServices(DynamicPropertyRegistry properties) {
        String databaseUrl = requiredEnvironment("SW331_TEST_POSTGRES_URL");
        String databaseUser = requiredEnvironment("SW331_TEST_POSTGRES_USER");
        String databasePassword = requiredEnvironment("SW331_TEST_POSTGRES_PASSWORD");
        String kafka = requiredEnvironment("SW331_TEST_KAFKA_BOOTSTRAP");
        String redisHost = requiredEnvironment("SW331_TEST_REDIS_HOST");
        String redisPort = requiredEnvironment("SW331_TEST_REDIS_PORT");
        String namespace = requiredEnvironment("SW331_TEST_CACHE_NAMESPACE");

        DriverManagerDataSource dataSource = new DriverManagerDataSource(databaseUrl, databaseUser, databasePassword);
        assertDisposableTargetsAreEmpty(dataSource, kafka, redisHost, Integer.parseInt(redisPort), namespace);
        ManualSqlTestSupport.initializeRedesignedPostgres(dataSource);
        properties.add("spring.datasource.url", () -> databaseUrl);
        properties.add("spring.datasource.username", () -> databaseUser);
        properties.add("spring.datasource.password", () -> databasePassword);
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.kafka.bootstrap-servers", () -> kafka);
        properties.add("spring.data.redis.host", () -> redisHost);
        properties.add("spring.data.redis.port", () -> Integer.parseInt(redisPort));
        properties.add("stockwellness.cache.namespace", () -> namespace);
        properties.add("spring.kafka.consumer.market-data-revision-derived-group-id", () -> DERIVED_GROUP);
        properties.add("spring.kafka.consumer.market-data-revision-cache-group-id", () -> CACHE_GROUP);
    }

    private static void assertDisposableTargetsAreEmpty(DriverManagerDataSource dataSource, String kafka,
            String redisHost, int redisPort, String namespace) {
        if (!"jdbc:postgresql://127.0.0.1:55433/stockwellness_331_it".equals(dataSource.getUrl())) {
            throw new IllegalStateException("Issue 331 test PostgreSQL URL is not the disposable target");
        }
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT current_database(), current_setting('server_version'),
                               (SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public')
                        """)) {
            if (!result.next() || !"stockwellness_331_it".equals(result.getString(1))
                    || !result.getString(2).startsWith("15.") || result.getLong(3) != 0) {
                throw new IllegalStateException("Issue 331 PostgreSQL identity or empty-schema preflight failed");
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Could not verify disposable Issue 331 PostgreSQL", ex);
        }
        if (!"127.0.0.1".equals(redisHost) || redisPort != 56380 || !namespace.startsWith("issue331-it-")
                || namespace.contains("::")) {
            throw new IllegalStateException("Issue 331 Redis host, port, or namespace is not isolated");
        }
        LettuceConnectionFactory redisFactory = new LettuceConnectionFactory(redisHost, redisPort);
        redisFactory.afterPropertiesSet();
        RedisConnection redisConnection = redisFactory.getConnection();
        try {
            if (!"PONG".equals(redisConnection.ping()) || redisConnection.serverCommands().dbSize() != 0) {
                throw new IllegalStateException("Issue 331 Redis identity or empty database preflight failed");
            }
        } finally {
            redisConnection.close();
            redisFactory.destroy();
        }
        if (!"127.0.0.1:59093".equals(kafka)) {
            throw new IllegalStateException("Issue 331 Kafka bootstrap is not the disposable target");
        }
        try (AdminClient admin = AdminClient.create(Map.of("bootstrap.servers", kafka))) {
            var nodes = admin.describeCluster().nodes().get(10, TimeUnit.SECONDS);
            var topics = admin.listTopics().names().get(10, TimeUnit.SECONDS);
            if (nodes.size() != 1 || nodes.iterator().next().id() != 331 || !topics.isEmpty()) {
                throw new IllegalStateException("Issue 331 Kafka identity or empty-topic preflight failed");
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Could not verify disposable Issue 331 Kafka", ex);
        }
    }

    @Test
    void final_revision_is_atomic_retried_idempotent_and_invalidates_cache_after_derived_commit() throws Exception {
        assertIsolatedRuntime();
        given(sectorLeadingStockPort.seriesIdsForStock(anyLong())).willReturn(List.of());
        stockId = jdbc.queryForObject("""
                INSERT INTO stock(market_code, ticker, name, market_type, currency, sector_code, sector_name, status)
                VALUES ('KOSPI', 'SW331IT', 'Issue 331 synthetic fixture', 'KOSPI', 'KRW', '0001', 'synthetic', 'ACTIVE')
                RETURNING id
                """, Long.class);
        seedFinalHistory();

        Stock stock = entityManager.find(Stock.class, stockId);
        derivedService.calculateStockIndicators(stockId, QUOTE_SCOPE, targetDate);
        baselineMa120 = readMa120();
        assertThat(baselineMa120).isNotNull();
        assertThat(readManifestRevision()).isEqualTo(1);

        Cache marketCache = requireCache("marketDashboard:v2");
        Cache portfolioCache = requireCache("portfolio_valuation");
        Cache memberCache = requireCache("member:v2");
        String marketKey = "issue-331-market";
        String portfolioKey = "issue-331-portfolio";
        String memberKey = "issue-331-member";
        marketCache.put(marketKey, "synthetic-old-market");
        portfolioCache.put(portfolioKey, "synthetic-old-portfolio");
        memberCache.put(memberKey, "member-preserved");
        assertRedisKeysAreNamespaced();

        EodStockPrice corrected = quote(stock, new BigDecimal("139.75"));
        rejectOutboxInserts();
        try {
            assertThatThrownBy(() -> sourceAdapter.upsert(List.of(corrected))).isInstanceOf(RuntimeException.class);
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS sw331_reject_outbox_insert ON outbox_event");
            jdbc.execute("DROP FUNCTION IF EXISTS sw331_reject_outbox_insert()");
        }
        assertThat(readSourceRevision()).isEqualTo(1);
        assertThat(readSourceClose()).isEqualByComparingTo("129.75");
        assertThat(outboxCount()).isZero();

        sourceAdapter.upsert(List.of(corrected));
        MarketDataRevisionEvent event = readOnlyOutboxEvent();
        assertThat(event.eventId()).isEqualTo("STOCK_PRICE:" + stockId + ":KRX:" + targetDate + ":2");
        assertThat(readSourceRevision()).isEqualTo(2);
        assertThat(outboxCount()).isEqualTo(1);
        sourceAdapter.upsert(List.of(corrected));
        assertThat(readSourceRevision()).isEqualTo(2);
        assertThat(outboxCount()).isEqualTo(1);
        rejectFirstDerivedWrite();
        rejectFirstCompletionOutboxInsert();

        String cacheNamespace = requiredEnvironment("SW331_TEST_CACHE_NAMESPACE");
        try (KafkaConsumer<String, String> auditConsumer = newKafkaAuditConsumer()) {
            awaitAuditAssignment(auditConsumer);
            MessageListenerContainer failedListener = listenerForGroup(DERIVED_GROUP);
            MessageListenerContainer retryListener = listenerForGroup(DERIVED_GROUP + "-derived-retry-1000");
            retryListener.stop();
            Awaitility.await().atMost(WAIT).until(() -> !retryListener.isRunning());
            failureGate.arm();
            // The broker rejects this record at the topic boundary; restore the limit before retrying.
            setBrokerMessageLimit("1");
            try {
                relay.relay();
                assertThat(outboxStatus(event)).isEqualTo("PENDING");
                assertThat(faultSwitch.sourceAttempts()).isEqualTo(1);
                assertThat(faultSwitch.publisherFailure()).hasRootCauseInstanceOf(RecordTooLargeException.class);
                System.out.printf("ISSUE340_BROKER_CAUSE eventId=%s%n", event.eventId());
                faultSwitch.publisherFailure().printStackTrace(System.out);
                assertThat(countAuditEvents(auditConsumer, event.eventId())).isZero();
                assertThat(jdbc.queryForObject("SELECT error_message FROM outbox_event WHERE payload = ?",
                        String.class, outboxPayload())).contains("not acknowledged by Kafka");
                System.out.printf("ISSUE340_BROKER_REJECT eventId=%s outbox=PENDING sourceRevision=%d derivedRevision=%d%n",
                        event.eventId(), readSourceRevision(), readManifestRevision());
            } finally {
                setBrokerMessageLimit(null);
            }

            relay.relay();
            awaitOutboxStatus(event, "COMPLETED");
            assertThat(faultSwitch.sourceAttempts()).isEqualTo(2);
            AtomicInteger observed = new AtomicInteger();
            Awaitility.await().atMost(WAIT).until(() -> {
                observed.addAndGet((int) countAuditEvents(auditConsumer, event.eventId()));
                return observed.get() > 0;
            });
            observed.addAndGet((int) countAuditEvents(auditConsumer, event.eventId()));
            assertThat(observed.get()).isEqualTo(1);
            System.out.printf("ISSUE340_BROKER_COUNTS eventId=%s publisherAttempts=%d brokerObservedAfterReject=0 brokerObservedAfterAck=%d%n",
                    event.eventId(), faultSwitch.sourceAttempts(), observed.get());
            assertThat(failureGate.awaitEntered(WAIT)).isTrue();
            assertThat(readManifestRevision()).isEqualTo(1);
            assertThat(completionOutboxCount(event)).isZero();
            assertThat(marketCache.get(marketKey).get()).isEqualTo("synthetic-old-market");
            ExecutorService stopper = Executors.newSingleThreadExecutor();
            try {
                var stopped = stopper.submit((Runnable) failedListener::stop);
                Awaitility.await().atMost(WAIT).until(() -> !failedListener.isRunning());
                failureGate.release();
                stopped.get(WAIT.toSeconds(), TimeUnit.SECONDS);
                assertThat(failedListener.isRunning()).isFalse();
                assertThat(readManifestRevision()).isEqualTo(1);
                assertThat(completionOutboxCount(event)).isZero();
                long attemptsBeforeRestart = revisionTrace.count(event.eventId());
                completionFailureGate.arm();
                retryListener.start();
                awaitListenerAssigned(retryListener);
                awaitListener(() -> revisionTrace.count(event.eventId()) > attemptsBeforeRestart);
                failedListener.start();
                awaitListenerAssigned(failedListener);
                System.out.printf("ISSUE340_CONSUMER_RESTART eventId=%s before=%d after=%d sourceRevision=%d%n",
                        event.eventId(), attemptsBeforeRestart, revisionTrace.count(event.eventId()), readSourceRevision());
            } finally {
                failureGate.release();
                stopper.shutdownNow();
            }
            assertThat(completionFailureGate.awaitEntered(WAIT)).isTrue();
            try {
                assertThat(jdbc.queryForObject("SELECT last_value FROM sw331_completion_failure_seq", Long.class))
                        .isGreaterThanOrEqualTo(1L);
                assertThat(readManifestRevision()).isEqualTo(1);
                assertThat(completionOutboxCount(event)).isZero();
                System.out.printf("ISSUE340_COMPLETION_ROLLBACK eventId=%s sourceRevision=%d derivedRevision=%d completionCount=%d%n",
                        event.eventId(), readSourceRevision(), readManifestRevision(), completionOutboxCount(event));
            } finally {
                completionFailureGate.release();
            }
            awaitDerivedRevision(2);
            awaitListener(() -> revisionTrace.count(event.eventId()) >= 3);
            assertThat(derivedCallTrace.count()).isGreaterThanOrEqualTo(3);
            assertThat(readManifestRevision()).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT last_value FROM sw331_derived_failure_seq", Long.class))
                    .isGreaterThanOrEqualTo(3L);
            assertThat(jdbc.queryForObject("SELECT last_value FROM sw331_completion_failure_seq", Long.class))
                    .isGreaterThanOrEqualTo(2L);
            awaitOutboxStatus(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, event, "PENDING");
            faultSwitch.armFailure();
            relay.relay();
            awaitOutboxStatus(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, event, "PENDING");
            assertThat(completionOutboxError(event)).contains("synthetic relay failure");
            assertThat(marketCache.get(marketKey).get()).isEqualTo("synthetic-old-market");
            assertThat(portfolioCache.get(portfolioKey).get()).isEqualTo("synthetic-old-portfolio");
            relay.relay();
            awaitOutboxStatus(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, event, "COMPLETED");
            assertThat(awaitKafkaEvent(auditConsumer, KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC,
                    event.eventId())).isEqualTo(event);
            assertThat(readMa120()).isNotEqualByComparingTo(baselineMa120);
            awaitCacheInvalidation(marketCache, portfolioCache, marketKey, portfolioKey);
            assertThat(memberCache.get(memberKey).get()).isEqualTo("member-preserved");

            MessageListenerContainer derivedListener = listenerForGroup(DERIVED_GROUP);
            derivedListener.stop();
            Awaitility.await().atMost(WAIT).until(() -> !derivedListener.isRunning());
            marketCache.put(marketKey, "synthetic-duplicate-market");
            portfolioCache.put(portfolioKey, "synthetic-duplicate-portfolio");
            kafkaTemplate.send(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC, event.eventId(), event)
                    .get(WAIT.toSeconds(), TimeUnit.SECONDS);
            assertThat(awaitKafkaEvent(auditConsumer, KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC, event.eventId()))
                    .isEqualTo(event);
            assertThat(marketCache.get(marketKey).get()).isEqualTo("synthetic-duplicate-market");
            assertThat(portfolioCache.get(portfolioKey).get()).isEqualTo("synthetic-duplicate-portfolio");
            long deliveriesBeforeRestart = revisionTrace.count(event.eventId());
            derivedListener.start();
            awaitListenerAssigned(derivedListener);
            awaitListener(() -> revisionTrace.count(event.eventId()) > deliveriesBeforeRestart);
            awaitOutboxStatus(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, event, "PENDING");
            relay.relay();
            awaitOutboxStatus(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, event, "COMPLETED");
            assertThat(awaitKafkaEvent(auditConsumer, KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC,
                    event.eventId())).isEqualTo(event);
            awaitCacheInvalidation(marketCache, portfolioCache, marketKey, portfolioKey);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM stock_technical_indicator WHERE stock_id = ?",
                    Integer.class, stockId)).isEqualTo(1);
            assertThat(readSourceRevision()).isEqualTo(2);
            assertThat(readManifestRevision()).isEqualTo(2);
            assertThat(memberCache.get(memberKey).get()).isEqualTo("member-preserved");
            System.out.printf("ISSUE331_PIPELINE_PASS eventId=%s sourceRevision=2 outbox=COMPLETED derivedRevision=2 baselineMa120=%s correctedMa120=%s deliveryAttempts=%d cache=invalidated member=preserved%n",
                    event.eventId(), baselineMa120, readMa120(), revisionTrace.count(event.eventId()));

            marketCache.put(marketKey, "synthetic-pre-race-market");
            portfolioCache.put(portfolioKey, "synthetic-pre-race-portfolio");
            commitGate.arm();
            EodStockPrice thirdRevision = quote(stock, new BigDecimal("149.75"));
            sourceAdapter.upsert(List.of(thirdRevision));
            MarketDataRevisionEvent raceEvent = readOnlyOutboxEvent();
            assertThat(raceEvent.eventId()).isEqualTo("STOCK_PRICE:" + stockId + ":KRX:" + targetDate + ":3");
            relay.relay();
            awaitOutboxStatus(raceEvent, "COMPLETED");
            assertThat(awaitKafkaEvent(auditConsumer, KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC,
                    raceEvent.eventId())).isEqualTo(raceEvent);
            try {
                assertThat(commitGate.awaitEntered(WAIT)).isTrue();
                marketCache.put(marketKey, "synthetic-stale-during-uncommitted-derived-write");
                portfolioCache.put(portfolioKey, "synthetic-stale-portfolio-during-uncommitted-derived-write");
                assertThat(completionOutboxCount(raceEvent)).isZero();
            } finally {
                commitGate.release();
            }

            awaitDerivedRevision(3);
            awaitListener(() -> revisionTrace.count(raceEvent.eventId()) >= 1);
            awaitOutboxStatus(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, raceEvent, "PENDING");
            relay.relay();
            awaitOutboxStatus(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, raceEvent, "COMPLETED");
            assertThat(awaitKafkaEvent(auditConsumer, KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC,
                    raceEvent.eventId())).isEqualTo(raceEvent);
            awaitCacheInvalidation(marketCache, portfolioCache, marketKey, portfolioKey);
            Object marketAfterCommit = marketCache.get(marketKey) == null ? null : marketCache.get(marketKey).get();
            Object portfolioAfterCommit = portfolioCache.get(portfolioKey) == null ? null
                    : portfolioCache.get(portfolioKey).get();
            System.out.printf("ISSUE331_RACE eventId=%s sourceRevision=%d derivedRevision=%d marketCache=%s portfolioCache=%s%n",
                    raceEvent.eventId(), readSourceRevision(), readManifestRevision(), marketAfterCommit, portfolioAfterCommit);
            assertThat(marketAfterCommit)
                    .as("market cache must not retain a value inserted before the derived PostgreSQL commit")
                    .isNull();
            assertThat(portfolioAfterCommit)
                    .as("portfolio cache must not retain a value inserted before the derived PostgreSQL commit")
                    .isNull();

            verifyIndexBreadthCorrectionPipeline(auditConsumer);
            verifySectorWeatherCorrectionPipeline(auditConsumer);
            verifyNetworkTimeoutRecovery(stock, auditConsumer);
            verifyJvmKillNineRecovery(auditConsumer);
        } finally {
            failureGate.release();
            completionFailureGate.release();
        }

    }

    private void assertIsolatedRuntime() throws Exception {
        assertThat(environment.getActiveProfiles()).contains("test");
        String database = jdbc.queryForObject("SELECT current_database()", String.class);
        String postgresVersion = jdbc.queryForObject("SHOW server_version", String.class);
        assertThat(database).isEqualTo("stockwellness_331_it");
        assertThat(postgresVersion).startsWith("15.");
        assertThat(redis.getConnectionFactory().getConnection().ping()).isEqualTo("PONG");
        assertThat(redis.keys("*")).isEmpty();
        String bootstrap = requiredEnvironment("SW331_TEST_KAFKA_BOOTSTRAP");
        try (AdminClient admin = AdminClient.create(Map.of("bootstrap.servers", bootstrap))) {
            assertThat(admin.describeCluster().nodes().get(10, TimeUnit.SECONDS)).hasSize(1);
        }
        cacheNamespace = requiredEnvironment("SW331_TEST_CACHE_NAMESPACE");
        assertThat(environment.getProperty("stockwellness.cache.namespace")).isEqualTo(cacheNamespace);
        System.out.printf("ISSUE331_ISOLATION profile=test db=%s postgres=%s redisNamespace=%s kafkaBootstrap=%s%n",
                database, postgresVersion, cacheNamespace, bootstrap);
    }

    private void verifyIndexBreadthCorrectionPipeline(KafkaConsumer<String, String> auditConsumer) throws Exception {
        long indexId = jdbc.queryForObject("""
                INSERT INTO market_index(index_key, name, kind, public_benchmark_code)
                VALUES ('SYNTHETIC:ISSUE331-KOSDAQ', 'Issue 331 synthetic KOSDAQ index', 'MARKET', '1001') RETURNING id
                """, Long.class);
        long seriesId = jdbc.queryForObject("""
                INSERT INTO index_series(index_id, provider, provider_namespace, provider_series_code,
                    calendar_market_code, return_basis, dividend_basis, currency, is_active, is_canonical)
                VALUES (?, 'SYNTHETIC', 'issue331', '1001', 'KOSDAQ', 'PRICE', 'NONE', 'KRW', true, true)
                RETURNING id
                """, Long.class, indexId);
        IndexSeries series = entityManager.find(IndexSeries.class, seriesId);
        LocalDate indexTargetDate = FIRST_DATE.plusDays(23);
        List<IndexDaily> history = new java.util.ArrayList<>();
        for (int offset = 0; offset < 24; offset++) {
            LocalDate date = FIRST_DATE.plusDays(offset);
            BigDecimal close = new BigDecimal("2500.00").add(BigDecimal.valueOf(offset * 2L));
            int advance = offset == 23 ? 10 : 100 + offset;
            int decline = offset == 23 ? 190 : 20 + offset * 2;
            history.add(IndexDaily.of(series, date, close.subtract(BigDecimal.ONE), close.add(BigDecimal.TEN),
                    close.subtract(BigDecimal.TEN), close, 10000L,
                    close.multiply(BigDecimal.valueOf(10000)), advance, 2 + offset % 3,
                    20 + offset, decline, 1 + offset % 2, null, null,
                    SOURCE, null, BenchmarkDataStatus.FINAL, null));
        }
        indexDailyPort.upsert(history);

        long firstSectorSeriesId = createIndexSeries("KOSDAQ:ISSUE331-SECTOR-1002", "Synthetic KOSDAQ sector 1002",
                "1002", "KOSDAQ", "SECTOR");
        long secondSectorSeriesId = createIndexSeries("KOSDAQ:ISSUE331-SECTOR-1003", "Synthetic KOSDAQ sector 1003",
                "1003", "KOSDAQ", "SECTOR");
        seedSectorWeatherHistory(firstSectorSeriesId, indexTargetDate, 20, 60, "SYNTHETIC:KOSDAQ-SECTOR-1002");
        seedSectorWeatherHistory(secondSectorSeriesId, indexTargetDate, 70, 10, "SYNTHETIC:KOSDAQ-SECTOR-1003");

        derivedService.calculateIndexWeather(seriesId, indexTargetDate);
        derivedService.calculateIndexWeather(firstSectorSeriesId, indexTargetDate);
        derivedService.calculateIndexWeather(secondSectorSeriesId, indexTargetDate);
        BigDecimal baselineWeather = readIndexWeatherScore(seriesId, indexTargetDate);
        assertThat(baselineWeather).isNotNull();
        assertThat(indexDailyReadPort.finalWeatherScore("1001", indexTargetDate))
                .contains(baselineWeather);
        assertThat(readIndexWeatherRevision(seriesId, indexTargetDate)).isEqualTo(1);

        BigDecimal firstSectorWeather = readIndexWeatherScore(firstSectorSeriesId, indexTargetDate);
        BigDecimal secondSectorWeather = readIndexWeatherScore(secondSectorSeriesId, indexTargetDate);
        var weatherService = new MarketWeatherService(indexDailyReadPort);
        var weatherController = new MarketWeatherController(weatherService);
        var beforeCorrection = weatherService.getLatestMarketWeather("KOSDAQ").orElseThrow();
        int expectedBefore = roundedSectorAverage(baselineWeather, firstSectorWeather, secondSectorWeather);
        assertThat(beforeCorrection.baseDate()).isEqualTo(indexTargetDate);
        assertThat(beforeCorrection.weatherScore()).isEqualTo(expectedBefore);
        var beforeApi = objectMapper.valueToTree(weatherController.getLatestWeather("KOSDAQ")).path("data");
        assertThat(beforeApi.path("marketType").asText()).isEqualTo("KOSDAQ");
        assertThat(beforeApi.path("weatherScore").asInt()).isEqualTo(expectedBefore);
        assertThat(beforeApi.path("topSectors").findValuesAsText("sectorCode"))
                .containsExactlyInAnyOrder("1001", "1002", "1003");
        assertThat(beforeApi.path("bottomSectors").findValuesAsText("sectorCode"))
                .containsExactlyInAnyOrder("1001", "1002", "1003");

        Cache marketCache = requireCache("marketDashboard:v2");
        Cache portfolioCache = requireCache("portfolio_valuation");
        Cache memberCache = requireCache("member:v2");
        String marketKey = "issue-331-index-market";
        String portfolioKey = "issue-331-index-portfolio";
        String memberKey = "issue-331-index-member";
        marketCache.put(marketKey, "synthetic-index-market");
        portfolioCache.put(portfolioKey, "synthetic-index-portfolio");
        memberCache.put(memberKey, "index-member-preserved");

        IndexDaily breadthCorrection = IndexDaily.of(series, indexTargetDate, null, null, null,
                new BigDecimal("2546.00"), null, null, 200, 10, 20, 20, 1,
                null, null, "SYNTHETIC:BREADTH:ISSUE-331", null, BenchmarkDataStatus.FINAL, null);
        concurrentlyUpsert(breadthCorrection);
        indexDailyPort.upsert(List.of(breadthCorrection));

        assertThat(readIndexPriceField(seriesId, indexTargetDate, "open_value")).isEqualByComparingTo("2545");
        assertThat(readIndexPriceField(seriesId, indexTargetDate, "high_value")).isEqualByComparingTo("2556");
        assertThat(readIndexPriceField(seriesId, indexTargetDate, "low_value")).isEqualByComparingTo("2536");
        assertThat(readIndexPriceField(seriesId, indexTargetDate, "close_value")).isEqualByComparingTo("2546");
        assertThat(jdbc.queryForObject("SELECT volume FROM index_daily WHERE series_id = ? AND trade_date = ?",
                Long.class, seriesId, indexTargetDate)).isEqualTo(10000L);
        assertThat(readIndexPriceField(seriesId, indexTargetDate, "trading_amount"))
                .isEqualByComparingTo("25460000");
        assertThat(readIndexIntegerField(seriesId, indexTargetDate, "advance_count")).isEqualTo(200);
        assertThat(readIndexIntegerField(seriesId, indexTargetDate, "upper_limit_count")).isEqualTo(10);
        assertThat(readIndexIntegerField(seriesId, indexTargetDate, "unchanged_count")).isEqualTo(20);
        assertThat(readIndexIntegerField(seriesId, indexTargetDate, "decline_count")).isEqualTo(20);
        assertThat(readIndexIntegerField(seriesId, indexTargetDate, "lower_limit_count")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT price_source FROM index_daily WHERE series_id = ? AND trade_date = ?",
                String.class, seriesId, indexTargetDate)).isEqualTo(SOURCE);
        assertThat(jdbc.queryForObject("SELECT price_status FROM index_daily WHERE series_id = ? AND trade_date = ?",
                String.class, seriesId, indexTargetDate)).isEqualTo("FINAL");
        assertThat(jdbc.queryForObject("SELECT revision FROM index_daily WHERE series_id = ? AND trade_date = ?",
                Integer.class, seriesId, indexTargetDate)).isEqualTo(2);
        MarketDataRevisionEvent event = readOnlyOutboxEvent(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC);
        assertThat(event.eventId()).isEqualTo("INDEX_PRICE:" + seriesId + ":-:" + indexTargetDate + ":2");
        assertThat(outboxEventCount(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC, event)).isEqualTo(1);

        relay.relay();
        awaitOutboxStatus(event, "COMPLETED");
        assertThat(awaitKafkaEvent(auditConsumer, KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC, event.eventId()))
                .isEqualTo(event);
        awaitListener(() -> revisionTrace.count(event.eventId()) >= 1);
        awaitListener(() -> readIndexWeatherRevision(seriesId, indexTargetDate) == 2);
        assertThat(readIndexWeatherCount(seriesId, indexTargetDate)).isEqualTo(1);
        assertThat(readIndexWeatherAdvanceCount(seriesId, indexTargetDate)).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT status FROM index_weather WHERE series_id = ? AND trade_date = ?",
                String.class, seriesId, indexTargetDate)).isEqualTo("FINAL");
        assertThat(jdbc.queryForObject("SELECT input_manifest ->> 'source' FROM index_weather WHERE series_id = ? AND trade_date = ?",
                String.class, seriesId, indexTargetDate)).isEqualTo("index_daily");
        assertThat(jdbc.queryForObject("SELECT input_manifest ->> 'seriesId' FROM index_weather WHERE series_id = ? AND trade_date = ?",
                String.class, seriesId, indexTargetDate)).isEqualTo(Long.toString(seriesId));
        assertThat(indexDailyReadPort.finalWeatherScore("1001", indexTargetDate)).isPresent();
        BigDecimal correctedWeather = readIndexWeatherScore(seriesId, indexTargetDate);
        var afterCorrection = weatherService.getLatestMarketWeather("KOSDAQ").orElseThrow();
        int expectedAfter = roundedSectorAverage(correctedWeather, firstSectorWeather, secondSectorWeather);
        assertThat(correctedWeather).isNotEqualByComparingTo(baselineWeather);
        assertThat(afterCorrection.weatherScore()).isEqualTo(expectedAfter);
        assertThat(afterCorrection.weatherScore()).isNotEqualTo(beforeCorrection.weatherScore());
        var afterApi = objectMapper.valueToTree(weatherController.getLatestWeather("KOSDAQ")).path("data");
        assertThat(afterApi.path("marketType").asText()).isEqualTo("KOSDAQ");
        assertThat(afterApi.path("baseDate").asText()).isEqualTo(indexTargetDate.toString());
        assertThat(afterApi.path("weatherScore").asInt()).isEqualTo(expectedAfter);
        assertThat(afterApi.path("topSectors").findValuesAsText("sectorCode"))
                .containsExactlyInAnyOrder("1001", "1002", "1003");
        assertThat(afterApi.path("bottomSectors").findValuesAsText("sectorCode"))
                .containsExactlyInAnyOrder("1001", "1002", "1003");
        assertThat(marketCache.get(marketKey).get()).isEqualTo("synthetic-index-market");
        assertThat(portfolioCache.get(portfolioKey).get()).isEqualTo("synthetic-index-portfolio");
        awaitOutboxStatus(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, event, "PENDING");
        relay.relay();
        awaitOutboxStatus(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, event, "COMPLETED");
        assertThat(awaitKafkaEvent(auditConsumer, KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC,
                event.eventId())).isEqualTo(event);
        awaitCacheInvalidation(marketCache, portfolioCache, marketKey, portfolioKey);
        assertThat(memberCache.get(memberKey).get()).isEqualTo("index-member-preserved");
        System.out.printf("ISSUE331_INDEX_BREADTH_PASS eventId=%s sourceRevision=2 weatherRevision=2 scoreBefore=%d scoreAfter=%d api=KOSDAQ average=verified completion=COMPLETED cache=invalidated%n",
                event.eventId(), beforeCorrection.weatherScore(), afterCorrection.weatherScore());
    }

    private void verifySectorWeatherCorrectionPipeline(KafkaConsumer<String, String> auditConsumer) throws Exception {
        long marketSeriesId = createIndexSeries("KOSPI:ISSUE331-MARKET", "Synthetic KOSPI market",
                "0001", "KOSPI", "MARKET");
        LocalDate indexTargetDate = FIRST_DATE.plusDays(23);
        seedSectorWeatherHistory(marketSeriesId, indexTargetDate, 50, 10, "SYNTHETIC:KOSPI-MARKET:ISSUE-331");
        derivedService.calculateIndexWeather(marketSeriesId, indexTargetDate);
        long firstSectorIndexId = jdbc.queryForObject("""
                INSERT INTO market_index(index_key, name, kind)
                VALUES ('KOSPI:ISSUE331-SECTOR-A', 'Synthetic sector A', 'SECTOR') RETURNING id
                """, Long.class);
        long firstSectorSeriesId = jdbc.queryForObject("""
                INSERT INTO index_series(index_id, provider, provider_namespace, provider_series_code,
                    calendar_market_code, return_basis, dividend_basis, currency, is_active, is_canonical)
                VALUES (?, 'SYNTHETIC', 'issue331', '0002', 'KOSPI', 'PRICE', 'NONE', 'KRW', true, true)
                RETURNING id
                """, Long.class, firstSectorIndexId);
        long secondSectorIndexId = jdbc.queryForObject("""
                INSERT INTO market_index(index_key, name, kind)
                VALUES ('KOSPI:ISSUE331-SECTOR-B', 'Synthetic sector B', 'SECTOR') RETURNING id
                """, Long.class);
        long secondSectorSeriesId = jdbc.queryForObject("""
                INSERT INTO index_series(index_id, provider, provider_namespace, provider_series_code,
                    calendar_market_code, return_basis, dividend_basis, currency, is_active, is_canonical)
                VALUES (?, 'SYNTHETIC', 'issue331', '0003', 'KOSPI', 'PRICE', 'NONE', 'KRW', true, true)
                RETURNING id
                """, Long.class, secondSectorIndexId);
        seedSectorWeatherHistory(firstSectorSeriesId, indexTargetDate, 0, 100, "SYNTHETIC:SECTOR-A:ISSUE-331");
        seedSectorWeatherHistory(secondSectorSeriesId, indexTargetDate, 20, 20, "SYNTHETIC:SECTOR-B:ISSUE-331");
        derivedService.calculateIndexWeather(firstSectorSeriesId, indexTargetDate);
        derivedService.calculateIndexWeather(secondSectorSeriesId, indexTargetDate);

        var weatherService = new MarketWeatherService(indexDailyReadPort);
        var weatherController = new MarketWeatherController(weatherService);
        var beforeCorrection = weatherService.getLatestMarketWeather("KOSPI").orElseThrow();
        BigDecimal firstSectorBefore = readIndexWeatherScore(firstSectorSeriesId, indexTargetDate);
        BigDecimal secondSectorBefore = readIndexWeatherScore(secondSectorSeriesId, indexTargetDate);
        assertThat(beforeCorrection.weatherScore()).isEqualTo(roundedSectorAverage(firstSectorBefore, secondSectorBefore));
        assertThat(objectMapper.valueToTree(weatherController.getLatestWeather("KOSPI"))
                .path("data").path("weatherScore").asInt()).isEqualTo(beforeCorrection.weatherScore());

        Cache marketCache = requireCache("marketDashboard:v2");
        Cache portfolioCache = requireCache("portfolio_valuation");
        Cache memberCache = requireCache("member:v2");
        String marketKey = "issue-331-sector-market";
        String portfolioKey = "issue-331-sector-portfolio";
        String memberKey = "issue-331-sector-member";
        marketCache.put(marketKey, "sector-weather-before-correction");
        portfolioCache.put(portfolioKey, "sector-portfolio-before-correction");
        memberCache.put(memberKey, "sector-member-preserved");

        IndexDaily correction = IndexDaily.of(entityManager.find(IndexSeries.class, firstSectorSeriesId),
                indexTargetDate, null, null, null, readIndexPriceField(firstSectorSeriesId, indexTargetDate, "close_value"),
                null, null, 500, 1, 1, 0, 0,
                null, null, "SYNTHETIC:SECTOR-A-CORRECTION:ISSUE-331", null,
                BenchmarkDataStatus.FINAL, null);
        indexDailyPort.upsert(List.of(correction));
        assertThat(weatherService.getLatestMarketWeather("KOSPI")).isEmpty();
        var unavailableApiResponse = objectMapper.valueToTree(weatherController.getLatestWeather("KOSPI"));
        assertThat(unavailableApiResponse.path("success").asBoolean()).isTrue();
        assertThat(unavailableApiResponse.hasNonNull("data")).isFalse();
        assertThat(jdbc.queryForObject("SELECT revision FROM index_daily WHERE series_id = ? AND trade_date = ?",
                Integer.class, firstSectorSeriesId, indexTargetDate)).isEqualTo(2);
        MarketDataRevisionEvent event = readOnlyOutboxEvent(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC);
        assertThat(event.sourceId()).isEqualTo(firstSectorSeriesId);
        assertThat(event.eventId()).isEqualTo("INDEX_PRICE:" + firstSectorSeriesId + ":-:" + indexTargetDate + ":2");

        relay.relay();
        awaitOutboxStatus(event, "COMPLETED");
        assertThat(awaitKafkaEvent(auditConsumer, KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC, event.eventId()))
                .isEqualTo(event);
        awaitListener(() -> revisionTrace.count(event.eventId()) >= 1);
        awaitListener(() -> readIndexWeatherRevision(firstSectorSeriesId, indexTargetDate) == 2);
        assertThat(readIndexWeatherCount(firstSectorSeriesId, indexTargetDate)).isEqualTo(1);
        assertThat(readIndexWeatherAdvanceCount(firstSectorSeriesId, indexTargetDate)).isEqualTo(500);
        BigDecimal firstSectorAfter = readIndexWeatherScore(firstSectorSeriesId, indexTargetDate);
        var afterCorrection = weatherService.getLatestMarketWeather("KOSPI").orElseThrow();
        assertThat(firstSectorAfter).isNotEqualByComparingTo(firstSectorBefore);
        assertThat(afterCorrection.weatherScore()).isEqualTo(roundedSectorAverage(firstSectorAfter, secondSectorBefore));
        assertThat(afterCorrection.weatherScore()).isNotEqualTo(beforeCorrection.weatherScore());
        assertThat(objectMapper.valueToTree(weatherController.getLatestWeather("KOSPI"))
                .path("data").path("weatherScore").asInt()).isEqualTo(afterCorrection.weatherScore());

        awaitOutboxStatus(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, event, "PENDING");
        relay.relay();
        awaitOutboxStatus(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, event, "COMPLETED");
        assertThat(awaitKafkaEvent(auditConsumer, KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC,
                event.eventId())).isEqualTo(event);
        awaitCacheInvalidation(marketCache, portfolioCache, marketKey, portfolioKey);
        assertThat(memberCache.get(memberKey).get()).isEqualTo("sector-member-preserved");
        System.out.printf("ISSUE331_SECTOR_WEATHER_PASS eventId=%s sectorRevision=2 weatherRevision=2 scoreBefore=%d scoreAfter=%d cache=invalidated%n",
                event.eventId(), beforeCorrection.weatherScore(), afterCorrection.weatherScore());
    }

    private void seedSectorWeatherHistory(long seriesId, LocalDate targetDate,
            int targetAdvanceCount, int targetDeclineCount, String source) {
        IndexSeries series = entityManager.find(IndexSeries.class, seriesId);
        List<IndexDaily> history = new java.util.ArrayList<>();
        for (int offset = 0; offset < 24; offset++) {
            LocalDate date = FIRST_DATE.plusDays(offset);
            BigDecimal close = BigDecimal.valueOf(100L + offset * 2L);
            int advance = offset == 23 ? targetAdvanceCount : 50 + offset;
            int decline = offset == 23 ? targetDeclineCount : 5;
            history.add(IndexDaily.of(series, date, close.subtract(BigDecimal.ONE), close.add(BigDecimal.TEN),
                    close.subtract(BigDecimal.TEN), close, 10000L, close.multiply(BigDecimal.valueOf(10000)),
                    advance, 0, 1, decline, 0, null, null, source, null, BenchmarkDataStatus.FINAL, null));
        }
        indexDailyPort.upsert(history);
    }

    private long createIndexSeries(String indexKey, String name, String code, String marketCode, String kind) {
        long indexId = "MARKET".equals(kind)
                ? jdbc.queryForObject("""
                        INSERT INTO market_index(index_key, name, kind, public_benchmark_code)
                        VALUES (?, ?, ?, ?) RETURNING id
                        """, Long.class, indexKey, name, kind, code)
                : jdbc.queryForObject("""
                        INSERT INTO market_index(index_key, name, kind)
                        VALUES (?, ?, ?) RETURNING id
                        """, Long.class, indexKey, name, kind);
        return jdbc.queryForObject("""
                INSERT INTO index_series(index_id, provider, provider_namespace, provider_series_code,
                    calendar_market_code, return_basis, dividend_basis, currency, is_active, is_canonical)
                VALUES (?, 'SYNTHETIC', 'issue331', ?, ?, 'PRICE', 'NONE', 'KRW', true, true)
                RETURNING id
                """, Long.class, indexId, code, marketCode);
    }

    private static int roundedSectorAverage(BigDecimal first, BigDecimal second) {
        return first.add(second).divide(BigDecimal.valueOf(2), 0, java.math.RoundingMode.HALF_UP).intValueExact();
    }

    private static int roundedSectorAverage(BigDecimal first, BigDecimal second, BigDecimal third) {
        return first.add(second).add(third)
                .divide(BigDecimal.valueOf(3), 0, java.math.RoundingMode.HALF_UP).intValueExact();
    }

    private void concurrentlyUpsert(IndexDaily correction) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<java.util.concurrent.Future<?>> writes = List.of(
                    executor.submit(() -> upsertAfterStart(ready, start, correction)),
                    executor.submit(() -> upsertAfterStart(ready, start, correction)));
            assertThat(ready.await(WAIT.toSeconds(), TimeUnit.SECONDS)).isTrue();
            start.countDown();
            writes.forEach(write -> {
                try {
                    write.get(WAIT.toSeconds(), TimeUnit.SECONDS);
                } catch (Exception ex) {
                    throw new IllegalStateException("Concurrent IndexDaily upsert failed", ex);
                }
            });
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private Void upsertAfterStart(CountDownLatch ready, CountDownLatch start, IndexDaily correction) {
        ready.countDown();
        try {
            if (!start.await(WAIT.toSeconds(), TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to start concurrent IndexDaily upsert");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted before concurrent IndexDaily upsert", ex);
        }
        indexDailyPort.upsert(List.of(correction));
        return null;
    }

    private BigDecimal readIndexPriceField(long seriesId, LocalDate date, String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM index_daily WHERE series_id = ? AND trade_date = ?",
                BigDecimal.class, seriesId, date);
    }

    private int readIndexIntegerField(long seriesId, LocalDate date, String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM index_daily WHERE series_id = ? AND trade_date = ?",
                Integer.class, seriesId, date);
    }

    private BigDecimal readIndexWeatherScore(long seriesId, LocalDate date) {
        return jdbc.queryForObject("SELECT score FROM index_weather WHERE series_id = ? AND trade_date = ?",
                BigDecimal.class, seriesId, date);
    }

    private int readIndexWeatherRevision(long seriesId, LocalDate date) {
        return jdbc.queryForObject("""
                SELECT (input.value ->> 'revision')::integer
                FROM index_weather weather
                CROSS JOIN LATERAL jsonb_array_elements(weather.input_manifest -> 'observations') input(value)
                WHERE weather.series_id = ? AND weather.trade_date = ? AND input.value ->> 'date' = ?
                """, Integer.class, seriesId, date, date.toString());
    }

    private int readIndexWeatherAdvanceCount(long seriesId, LocalDate date) {
        return jdbc.queryForObject("""
                SELECT (input.value ->> 'advanceCount')::integer
                FROM index_weather weather
                CROSS JOIN LATERAL jsonb_array_elements(weather.input_manifest -> 'observations') input(value)
                WHERE weather.series_id = ? AND weather.trade_date = ? AND input.value ->> 'date' = ?
                """, Integer.class, seriesId, date, date.toString());
    }

    private int readIndexWeatherCount(long seriesId, LocalDate date) {
        return jdbc.queryForObject("SELECT count(*) FROM index_weather WHERE series_id = ? AND trade_date = ?",
                Integer.class, seriesId, date);
    }

    private void seedFinalHistory() {
        targetDate = FIRST_DATE.plusDays(119);
        for (int offset = 0; offset < 120; offset++) {
            LocalDate date = FIRST_DATE.plusDays(offset);
            BigDecimal close = new BigDecimal("100.00").add(new BigDecimal("0.25").multiply(BigDecimal.valueOf(offset)));
            jdbc.update("""
                    INSERT INTO stock_price_daily(stock_id, trade_date, quote_scope, open_price, high_price,
                        low_price, close_price, volume, trading_amount, data_status, source, collected_at, revision)
                    VALUES (?, ?, 'KRX', ?, ?, ?, ?, 1000, ?, 'FINAL', 'SYNTHETIC:FINAL:ISSUE-331', now(), 1)
                    """, stockId, date, close.subtract(BigDecimal.ONE), close.add(BigDecimal.ONE),
                    close.subtract(new BigDecimal("2.00")), close, close.multiply(BigDecimal.valueOf(1000)));
        }
    }

    private EodStockPrice quote(Stock stock, BigDecimal close) {
        return EodStockPrice.of(stock, targetDate, QUOTE_SCOPE, close.subtract(BigDecimal.ONE),
                close.add(BigDecimal.ONE), close.subtract(new BigDecimal("2.00")), close, 1000L,
                close.multiply(BigDecimal.valueOf(1000)), SOURCE, "FINAL");
    }

    private void setBrokerMessageLimit(String value) throws Exception {
        ConfigResource topic = new ConfigResource(ConfigResource.Type.TOPIC, KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC);
        AlterConfigOp operation = new AlterConfigOp(new ConfigEntry("max.message.bytes", value),
                value == null ? AlterConfigOp.OpType.DELETE : AlterConfigOp.OpType.SET);
        try (AdminClient admin = AdminClient.create(Map.of("bootstrap.servers", requiredEnvironment("SW331_TEST_KAFKA_BOOTSTRAP")))) {
            admin.incrementalAlterConfigs(Map.of(topic, List.of(operation))).all().get(10, TimeUnit.SECONDS);
        }
    }

    private void rejectOutboxInserts() {
        jdbc.execute("""
                CREATE FUNCTION sw331_reject_outbox_insert() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'synthetic outbox insert failure'; END;
                $$
                """);
        jdbc.execute("""
                CREATE TRIGGER sw331_reject_outbox_insert BEFORE INSERT ON outbox_event
                FOR EACH ROW EXECUTE FUNCTION sw331_reject_outbox_insert()
                """);
    }

    private void rejectFirstDerivedWrite() {
        jdbc.execute("CREATE SEQUENCE sw331_derived_failure_seq START WITH 1");
        jdbc.execute("""
                CREATE FUNCTION sw331_reject_first_derived_write() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF nextval('sw331_derived_failure_seq') = 1 THEN
                        RAISE EXCEPTION 'synthetic derived persistence failure';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """);
        jdbc.execute("""
                CREATE TRIGGER sw331_reject_first_derived_write BEFORE UPDATE ON stock_technical_indicator
                FOR EACH ROW EXECUTE FUNCTION sw331_reject_first_derived_write()
                """);
    }

    private void rejectFirstCompletionOutboxInsert() {
        jdbc.execute("CREATE SEQUENCE sw331_completion_failure_seq START WITH 1");
        jdbc.execute("""
                CREATE FUNCTION sw331_reject_first_completion_outbox_insert() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.topic = 'market-data-revision-completed'
                            AND nextval('sw331_completion_failure_seq') = 1 THEN
                        RAISE EXCEPTION 'synthetic completion outbox failure';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """);
        jdbc.execute("""
                CREATE TRIGGER sw331_reject_first_completion_outbox_insert BEFORE INSERT ON outbox_event
                FOR EACH ROW EXECUTE FUNCTION sw331_reject_first_completion_outbox_insert()
                """);
    }

    private MarketDataRevisionEvent readOnlyOutboxEvent() throws Exception {
        return readOnlyOutboxEvent(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC);
    }

    private MarketDataRevisionEvent readOnlyOutboxEvent(String topic) throws Exception {
        return objectMapper.readValue(outboxPayload(topic), MarketDataRevisionEvent.class);
    }

    private String outboxPayload() {
        return outboxPayload(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC);
    }

    private String outboxPayload(String topic) {
        return jdbc.queryForObject("SELECT payload FROM outbox_event WHERE topic = ? ORDER BY id DESC LIMIT 1",
                String.class, topic);
    }

    private int outboxCount() {
        return jdbc.queryForObject("SELECT count(*) FROM outbox_event", Integer.class);
    }

    private String outboxStatus(MarketDataRevisionEvent event) {
        return outboxStatus(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC, event);
    }

    private String outboxStatus(String topic, MarketDataRevisionEvent event) {
        return jdbc.queryForObject("SELECT status FROM outbox_event WHERE topic = ? AND payload = ? "
                        + "ORDER BY id DESC LIMIT 1", String.class, topic, payloadFor(topic, event));
    }

    private String completionOutboxError(MarketDataRevisionEvent event) {
        return jdbc.queryForObject("SELECT error_message FROM outbox_event WHERE topic = ? AND payload = ? "
                        + "ORDER BY id DESC LIMIT 1", String.class, KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC,
                payloadFor(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, event));
    }

    private int completionOutboxCount(MarketDataRevisionEvent event) {
        return jdbc.queryForObject("SELECT count(*) FROM outbox_event WHERE topic = ? AND payload LIKE ?",
                Integer.class, KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, eventIdPattern(event));
    }

    private String payloadFor(MarketDataRevisionEvent event) {
        return payloadFor(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC, event);
    }

    private String payloadFor(String topic, MarketDataRevisionEvent event) {
        return jdbc.queryForObject("SELECT payload FROM outbox_event WHERE topic = ? AND payload LIKE ? "
                        + "ORDER BY id DESC LIMIT 1", String.class, topic, eventIdPattern(event));
    }

    private int outboxEventCount(String topic, MarketDataRevisionEvent event) {
        return jdbc.queryForObject("SELECT count(*) FROM outbox_event WHERE topic = ? AND payload LIKE ?",
                Integer.class, topic, eventIdPattern(event));
    }

    private String eventIdPattern(MarketDataRevisionEvent event) {
        return "%\"eventId\":\"" + event.eventId() + "\"%";
    }

    private void awaitOutboxStatus(MarketDataRevisionEvent event, String status) {
        awaitOutboxStatus(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC, event, status);
    }

    private void awaitOutboxStatus(String topic, MarketDataRevisionEvent event, String status) {
        Awaitility.await().atMost(WAIT).untilAsserted(() -> assertThat(outboxStatus(topic, event)).isEqualTo(status));
    }

    private int readSourceRevision() {
        return jdbc.queryForObject("SELECT revision FROM stock_price_daily WHERE stock_id = ? AND trade_date = ? AND quote_scope = ?",
                Integer.class, stockId, targetDate, QUOTE_SCOPE);
    }

    private BigDecimal readSourceClose() {
        return jdbc.queryForObject("SELECT close_price FROM stock_price_daily WHERE stock_id = ? AND trade_date = ? AND quote_scope = ?",
                BigDecimal.class, stockId, targetDate, QUOTE_SCOPE);
    }

    private BigDecimal readMa120() {
        return jdbc.queryForObject("SELECT ma120 FROM stock_technical_indicator WHERE stock_id = ? AND trade_date = ? AND quote_scope = ?",
                BigDecimal.class, stockId, targetDate, QUOTE_SCOPE);
    }

    private int readManifestRevision() {
        return jdbc.queryForObject("""
                SELECT (observation.value ->> 'revision')::integer
                FROM stock_technical_indicator indicator
                CROSS JOIN LATERAL jsonb_array_elements(indicator.input_manifest -> 'observations') observation(value)
                WHERE indicator.stock_id = ? AND indicator.trade_date = ?
                  AND observation.value ->> 'date' = ?
                """, Integer.class, stockId, targetDate, targetDate.toString());
    }

    private void awaitDerivedRevision(int revision) {
        Awaitility.await().atMost(WAIT).untilAsserted(() -> assertThat(readManifestRevision()).isEqualTo(revision));
    }

    private Cache requireCache(String name) {
        Cache cache = cacheManager.getCache(name);
        assertThat(cache).as("cache %s", name).isNotNull();
        return cache;
    }

    private void assertRedisKeysAreNamespaced() {
        Set<String> keys = redis.keys("*");
        assertThat(keys).isNotEmpty().allMatch(key -> key.startsWith(cacheNamespace + "::"));
    }

    private void awaitCacheInvalidation(Cache market, Cache portfolio, String marketKey, String portfolioKey) {
        Awaitility.await().atMost(WAIT).untilAsserted(() -> {
            assertThat(market.get(marketKey)).isNull();
            assertThat(portfolio.get(portfolioKey)).isNull();
        });
    }

    private void awaitListener(Supplier<Boolean> condition) {
        Awaitility.await().atMost(WAIT).pollInterval(Duration.ofMillis(100)).until(condition::get);
    }

    private MessageListenerContainer listenerForGroup(String groupId) {
        return listenerRegistry.getListenerContainers().stream()
                .filter(container -> groupId.equals(container.getGroupId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No Kafka listener for group " + groupId));
    }

    private void awaitListenerAssigned(MessageListenerContainer listener) {
        Awaitility.await().atMost(WAIT).pollInterval(Duration.ofMillis(100))
                .until(() -> listener.getAssignedPartitions() != null && listener.getAssignedPartitions().size() == 1);
    }

    private KafkaConsumer<String, String> newKafkaAuditConsumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, requiredEnvironment("SW331_TEST_KAFKA_BOOTSTRAP"));
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "issue-331-audit-" + UUID.randomUUID());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaConsumer<>(properties);
    }

    private void awaitAuditAssignment(KafkaConsumer<String, String> consumer) {
        consumer.subscribe(List.of(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC,
                KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC));
        Awaitility.await().atMost(WAIT).pollInterval(Duration.ofMillis(100)).until(() -> {
            consumer.poll(Duration.ofMillis(100));
            return consumer.assignment().size() == 2;
        });
    }

    private long countAuditEvents(KafkaConsumer<String, String> consumer, String eventId) {
        long count = 0;
        for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofSeconds(1))
                .records(KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC)) {
            if (eventId.equals(record.key())) count++;
        }
        return count;
    }

    private MarketDataRevisionEvent awaitKafkaEvent(KafkaConsumer<String, String> consumer, String topic,
            String eventId) throws Exception {
        AtomicReference<ConsumerRecord<String, String>> match = new AtomicReference<>();
        Awaitility.await().atMost(WAIT).pollInterval(Duration.ofMillis(100)).until(() -> {
            consumer.poll(Duration.ofMillis(100)).forEach(record -> {
                if (topic.equals(record.topic()) && eventId.equals(record.key())) match.set(record);
            });
            return match.get() != null;
        });
        assertThat(match.get().key()).isEqualTo(eventId);
        return objectMapper.readValue(match.get().value(), MarketDataRevisionEvent.class);
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing isolated test setting " + name);
        return value;
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = BatchAutoConfiguration.class)
    @EntityScan(basePackages = "org.stockwellness.domain")
    @EnableJpaRepositories(basePackages = {
            "org.stockwellness.adapter.out.persistence.outbox",
            "org.stockwellness.adapter.out.persistence.stock.repository"
    })
    @Import({
            ApiRedisConfig.class,
            JpaConfig.class,
            QueryDslConfig.class,
            IndexDailyAdapter.class,
            IndexDailyReadAdapter.class,
            IndexWeatherAdapter.class,
            EodStockPriceAdapter.class,
            OutboxAdapter.class,
            StockPriceReadAdapter.class,
            TechnicalIndicatorAdapter.class,
            MarketDerivedDataService.class,
            MarketDataRevisionService.class,
            OutboxRelay.class,
            KafkaOutboxEventAdapter.class,
            MarketDataRevisionConsumer.class,
            MarketDataRevisionCacheConsumer.class,
            KafkaTopicConfig.class,
            TestBeans.class
    })
    static class IntegrationApplication {}

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean FaultSwitch faultSwitch() { return new FaultSwitch(); }
        @Bean CommitGate commitGate() { return new CommitGate(); }
        @Bean RevisionTrace revisionTrace() { return new RevisionTrace(); }
        @Bean DerivedCallTrace derivedCallTrace() { return new DerivedCallTrace(); }
        @Bean @Primary FailureGate failureGate() { return new FailureGate(); }
        @Bean CompletionFailureGate completionFailureGate() { return new CompletionFailureGate(); }

        @Bean
        @Primary
        OutboxEventPublisherPort retryingPublisher(KafkaOutboxEventAdapter delegate, FaultSwitch faultSwitch) {
            return (topic, payload) -> {
                faultSwitch.record(topic);
                if (faultSwitch.consumeFailure()) throw new IllegalStateException("synthetic relay failure");
                try {
                    delegate.publish(topic, payload);
                } catch (RuntimeException ex) {
                    faultSwitch.recordFailure(ex);
                    throw ex;
                }
            };
        }

        @Bean
        @Primary
        MarketDataRevisionUseCase tracingRevisionUseCase(MarketDataRevisionService delegate, RevisionTrace trace, FailureGate failureGate,
                CompletionFailureGate completionFailureGate) {
            return event -> {
                trace.record(event);
                try {
                    delegate.recalculate(event);
                } catch (RuntimeException ex) {
                    failureGate.afterFailure();
                    completionFailureGate.afterFailure();
                    throw ex;
                }
            };
        }

        @Bean
        @Primary
        MarketDerivedDataUseCase commitGatedDerivedService(
                MarketDerivedDataService delegate, CommitGate commitGate, DerivedCallTrace calls) {
            return new MarketDerivedDataUseCase() {
                @Override
                @Transactional
                public void calculateStockIndicators(long id, String scope, LocalDate date) {
                    calls.record();
                    delegate.calculateStockIndicators(id, scope, date);
                    commitGate.afterWrite();
                }

                @Override public void calculateIndexIndicators(long id, LocalDate date) {
                    delegate.calculateIndexIndicators(id, date);
                }

                @Override public void calculateIndexWeather(long id, LocalDate date) {
                    delegate.calculateIndexWeather(id, date);
                }

                @Override public void calculateSectorLeadingStocks(long id, LocalDate date) {
                    delegate.calculateSectorLeadingStocks(id, date);
                }
            };
        }
    }

    static class FaultSwitch {
        private final AtomicBoolean failNext = new AtomicBoolean();
        private final AtomicInteger sourceAttempts = new AtomicInteger();
        private final AtomicReference<RuntimeException> publisherFailure = new AtomicReference<>();
        void recordFailure(RuntimeException ex) { publisherFailure.set(ex); }
        RuntimeException publisherFailure() { return publisherFailure.get(); }
        void record(String topic) { if (KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC.equals(topic)) sourceAttempts.incrementAndGet(); }
        int sourceAttempts() { return sourceAttempts.get(); }
        void armFailure() { failNext.set(true); }
        boolean consumeFailure() { return failNext.compareAndSet(true, false); }
    }

    static class RevisionTrace {
        private final List<String> eventIds = new CopyOnWriteArrayList<>();
        void record(MarketDataRevisionEvent event) { eventIds.add(event.eventId()); }
        long count(String eventId) { return eventIds.stream().filter(eventId::equals).count(); }
    }

    static class DerivedCallTrace {
        private final AtomicInteger calls = new AtomicInteger();
        void record() { calls.incrementAndGet(); }
        int count() { return calls.get(); }
    }

    static class FailureGate {
        private final AtomicBoolean armed = new AtomicBoolean();
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch released = new CountDownLatch(1);
        void arm() { armed.set(true); }
        void afterFailure() {
            if (!armed.compareAndSet(true, false)) return;
            entered.countDown();
            try {
                if (!released.await(WAIT.toSeconds(), TimeUnit.SECONDS)) throw new IllegalStateException("failure gate timed out");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        }
        boolean awaitEntered(Duration timeout) throws InterruptedException { return entered.await(timeout.toMillis(), TimeUnit.MILLISECONDS); }
        void release() { released.countDown(); }
    }

    static class CompletionFailureGate extends FailureGate {}

    static class CommitGate {
        private final AtomicBoolean armed = new AtomicBoolean();
        private volatile CountDownLatch entered = new CountDownLatch(0);
        private volatile CountDownLatch released = new CountDownLatch(0);

        void arm() {
            entered = new CountDownLatch(1);
            released = new CountDownLatch(1);
            armed.set(true);
        }

        void afterWrite() {
            if (!armed.compareAndSet(true, false)) return;
            entered.countDown();
            try {
                if (!released.await(WAIT.toSeconds(), TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to release derived commit gate");
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while holding derived commit gate", ex);
            }
        }

        boolean awaitEntered(Duration timeout) throws InterruptedException {
            return entered.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        void release() { released.countDown(); }
    }

    private void verifyNetworkTimeoutRecovery(Stock stock, KafkaConsumer<String, String> auditConsumer) throws Exception {
        EodStockPrice fourthRevision = quote(stock, new BigDecimal("159.75"));
        sourceAdapter.upsert(List.of(fourthRevision));
        MarketDataRevisionEvent event = readOnlyOutboxEvent();
        assertThat(event.eventId()).isEqualTo("STOCK_PRICE:" + stockId + ":KRX:" + targetDate + ":4");
        assertThat(readSourceRevision()).isEqualTo(4);

        try (ServerSocket blackhole = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            ExecutorService acceptPool = Executors.newSingleThreadExecutor();
            List<Socket> acceptedSockets = new CopyOnWriteArrayList<>();
            acceptPool.submit(() -> {
                while (!blackhole.isClosed()) {
                    try {
                        acceptedSockets.add(blackhole.accept());
                    } catch (Exception ignored) {
                        break;
                    }
                }
            });
            Map<String, Object> props = Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:" + blackhole.getLocalPort(),
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                    ProducerConfig.MAX_BLOCK_MS_CONFIG, 500,
                    ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 500,
                    ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 1000,
                    ProducerConfig.RETRIES_CONFIG, 0
            );
            DefaultKafkaProducerFactory<String, Object> timeoutFactory = new DefaultKafkaProducerFactory<>(props);
            try {
                KafkaTemplate<String, Object> timeoutTemplate = new KafkaTemplate<>(timeoutFactory);
                KafkaOutboxEventAdapter timeoutAdapter = new KafkaOutboxEventAdapter(timeoutTemplate, objectMapper);
                OutboxRelay timeoutRelay = new OutboxRelay(outboxPort, timeoutAdapter);

                new TransactionTemplate(transactionManager).executeWithoutResult(status -> timeoutRelay.relay());

                assertThat(outboxStatus(event)).isEqualTo("PENDING");
                assertThat(jdbc.queryForObject("SELECT error_message FROM outbox_event WHERE payload LIKE ?",
                        String.class, "%" + event.eventId() + "%"))
                        .containsAnyOf("Send failed", "not acknowledged by Kafka");
                assertThat(countAuditEvents(auditConsumer, event.eventId())).isZero();
                System.out.printf("ISSUE340_NETWORK_TIMEOUT eventId=%s outbox=PENDING blackholePort=%d%n",
                        event.eventId(), blackhole.getLocalPort());
            } finally {
                timeoutFactory.destroy();
                for (Socket socket : acceptedSockets) {
                    try {
                        socket.close();
                    } catch (Exception ignored) {
                    }
                }
                acceptPool.shutdownNow();
            }
        }

        relay.relay();
        awaitOutboxStatus(event, "COMPLETED");
        assertThat(awaitKafkaEvent(auditConsumer, KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC, event.eventId()))
                .isEqualTo(event);
        awaitDerivedRevision(4);
        awaitOutboxStatus(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, event, "PENDING");
        relay.relay();
        awaitOutboxStatus(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, event, "COMPLETED");
        assertThat(awaitKafkaEvent(auditConsumer, KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, event.eventId()))
                .isEqualTo(event);
        System.out.printf("ISSUE340_NETWORK_TIMEOUT_RECOVERED eventId=%s outbox=COMPLETED derivedRevision=%d%n",
                event.eventId(), readManifestRevision());
    }

    private void verifyJvmKillNineRecovery(KafkaConsumer<String, String> auditConsumer) throws Exception {
        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String eventId = "STOCK_PRICE:" + stockId + ":KRX:" + targetDate + ":5";
        String payload = objectMapper.writeValueAsString(
                MarketDataRevisionEvent.stock(stockId, targetDate, QUOTE_SCOPE, 5));

        Process process = new ProcessBuilder(
                javaBin,
                "-cp", System.getProperty("java.class.path"),
                KillNineOutboxWorker.class.getName(),
                requiredEnvironment("SW331_TEST_POSTGRES_URL"),
                requiredEnvironment("SW331_TEST_POSTGRES_USER"),
                requiredEnvironment("SW331_TEST_POSTGRES_PASSWORD"),
                String.valueOf(stockId),
                targetDate.toString(),
                QUOTE_SCOPE,
                "169.75",
                KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC,
                payload)
                .redirectErrorStream(true)
                .start();

        int exitCode;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            assertThat(line).isEqualTo("READY_FOR_KILL9");
            process.destroyForcibly();
            exitCode = process.waitFor();
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }

        assertThat(exitCode).isNotZero();
        assertThat(readSourceRevision()).isEqualTo(5);
        assertThat(readSourceClose()).isEqualByComparingTo("169.75");
        MarketDataRevisionEvent event = readOnlyOutboxEvent();
        assertThat(event.eventId()).isEqualTo(eventId);
        assertThat(outboxStatus(event)).isEqualTo("PENDING");
        assertThat(countAuditEvents(auditConsumer, eventId)).isZero();
        System.out.printf("ISSUE340_JVM_KILL9 eventId=%s exitCode=%d outbox=PENDING sourceRevision=%d%n",
                eventId, exitCode, readSourceRevision());

        relay.relay();
        awaitOutboxStatus(event, "COMPLETED");
        assertThat(awaitKafkaEvent(auditConsumer, KafkaTopicConfig.MARKET_DATA_REVISION_TOPIC, eventId))
                .isEqualTo(event);
        awaitDerivedRevision(5);
        awaitOutboxStatus(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, event, "PENDING");
        relay.relay();
        awaitOutboxStatus(KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, event, "COMPLETED");
        assertThat(awaitKafkaEvent(auditConsumer, KafkaTopicConfig.MARKET_DATA_REVISION_COMPLETED_TOPIC, eventId))
                .isEqualTo(event);
        System.out.printf("ISSUE340_JVM_KILL9_RECOVERED eventId=%s outbox=COMPLETED derivedRevision=%d%n",
                eventId, readManifestRevision());
    }

    public static class KillNineOutboxWorker {
        public static void main(String[] args) throws Exception {
            String url = args[0];
            String user = args[1];
            String password = args[2];
            long stockId = Long.parseLong(args[3]);
            LocalDate tradeDate = LocalDate.parse(args[4]);
            String quoteScope = args[5];
            BigDecimal closePrice = new BigDecimal(args[6]);
            String topic = args[7];
            String payload = args[8];

            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                conn.setAutoCommit(false);
                try (PreparedStatement updatePrice = conn.prepareStatement(
                        "UPDATE stock_price_daily SET open_price = ?, high_price = ?, low_price = ?, close_price = ?, revision = 5 WHERE stock_id = ? AND trade_date = ? AND quote_scope = ?");
                     PreparedStatement insertOutbox = conn.prepareStatement(
                             "INSERT INTO outbox_event(topic, payload, status, created_at, updated_at) VALUES (?, ?, 'PENDING', NOW(), NOW())")) {
                    updatePrice.setBigDecimal(1, closePrice);
                    updatePrice.setBigDecimal(2, closePrice.add(BigDecimal.ONE));
                    updatePrice.setBigDecimal(3, closePrice.subtract(BigDecimal.ONE));
                    updatePrice.setBigDecimal(4, closePrice);
                    updatePrice.setLong(5, stockId);
                    updatePrice.setObject(6, tradeDate);
                    updatePrice.setString(7, quoteScope);
                    if (updatePrice.executeUpdate() != 1) {
                        throw new IllegalStateException("Expected 1 stock_price_daily row updated");
                    }
                    insertOutbox.setString(1, topic);
                    insertOutbox.setString(2, payload);
                    insertOutbox.executeUpdate();
                    conn.commit();
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                }
            }
            System.out.println("READY_FOR_KILL9");
            System.out.flush();
            Thread.sleep(60_000);
        }
    }
}
