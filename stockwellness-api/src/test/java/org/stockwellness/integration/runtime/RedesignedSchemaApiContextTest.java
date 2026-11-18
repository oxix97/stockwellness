package org.stockwellness.integration.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.StockwellnessApiApplication;
import org.stockwellness.adapter.out.persistence.member.MemberRepository;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioRepository;
import org.stockwellness.application.port.in.auth.AuthUseCase;
import org.stockwellness.application.port.in.auth.command.LoginCommand;
import org.stockwellness.application.port.in.portfolio.AiAdvisorUseCase;
import org.stockwellness.application.port.out.auth.RefreshTokenPort;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.support.ManualSqlTestSupport;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/** Starts the complete API against the canonical schema with Hibernate validation enabled. */
@ExtendWith(RedesignedSchemaApiContextTest.DockerAvailable.class)
@SpringBootTest(
        classes = StockwellnessApiApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.sql.init.mode=never",
                "spring.docker.compose.enabled=false",
                "spring.kafka.admin.auto-create=false",
                "spring.kafka.listener.auto-startup=false",
                "spring.kafka.bootstrap-servers=localhost:9092",
                "spring.cache.type=none",
                "management.health.redis.enabled=false",
                "management.health.kafka.enabled=false",
                "KAKAO_CLIENT_ID=dummy-kakao-client-id",
                "KAKAO_CLIENT_SECRET=dummy-kakao-client-secret",
                "GOOGLE_CLIENT_ID=dummy-google-client-id",
                "GOOGLE_CLIENT_SECRET=dummy-google-client-secret",
                "OPENAI_API_KEY=dummy-openai-key",
                "JWT_SECRET=dGhpcy1pcy10ZXN0LXNlY3JldC1rZXktZm9yLXN0b2Nrd2VsbG5lc3MtcHJvamVjdC13aGljaC1pcy1zYWZl",
                "KIS_APP_KEY=dummy-kis-key",
                "KIS_APP_SECRET=dummy-kis-secret",
                "REDIRECT_URL=http://localhost:5173/auth/callback",
                "SLACK_WEBHOOK_URL_API="
        })
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class RedesignedSchemaApiContextTest {
    private static PostgreSQLContainer<?> postgres;

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthUseCase authUseCase;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PortfolioRepository portfolioRepository;

    @MockitoBean private LettuceConnectionFactory redisConnectionFactory;
    @MockitoBean private KafkaTemplate<String, Object> kafkaTemplate;
    @MockitoBean private KafkaAdmin kafkaAdmin;
    @MockitoBean private RefreshTokenPort refreshTokenPort;
    @MockitoBean private CacheManager cacheManager;
    @MockitoBean private AiAdvisorUseCase aiAdvisorUseCase;

    @BeforeEach
    void mockRedisBackedCaches() {
        given(cacheManager.getCache(anyString()))
                .willAnswer(invocation -> new ConcurrentMapCache(invocation.getArgument(0)));
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        postgres = new PostgreSQLContainer<>("postgres:15-alpine");
        postgres.start();
        ManualSqlTestSupport.initializeRedesignedPostgres(
                new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @AfterAll
    static void stopPostgres() {
        if (postgres != null) postgres.stop();
    }

    @Test
    void complete_api_context_persists_member_preferences_and_portfolio_description() {
        Member member = Member.register("canonical-schema@example.test", "canonical-api", LoginType.KAKAO);
        member.updateNotifications(false, true, true);
        Member savedMember = memberRepository.saveAndFlush(member);

        Member loadedMember = memberRepository.findMemberWithIdentities(savedMember.getId()).orElseThrow();
        assertThat(loadedMember.getStatus().name()).isEqualTo("ACTIVE");
        assertThat(loadedMember.isNotificationRebalancing()).isFalse();
        assertThat(loadedMember.isNotificationMarketAlert()).isTrue();
        assertThat(loadedMember.isNotificationNewListing()).isTrue();

        Portfolio savedPortfolio = portfolioRepository.saveAndFlush(
                Portfolio.create(savedMember.getId(), "canonical portfolio", "description survives"));
        assertThat(portfolioRepository.findById(savedPortfolio.getId()).orElseThrow().getDescription())
                .isEqualTo("description survives");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema='public' and table_type='BASE TABLE'",
                Integer.class)).isEqualTo(37);
        assertThat(jdbcTemplate.queryForObject("select nextval('batch_job_seq')", Long.class)).isEqualTo(1L);
    }

    @Test
    void canonical_stock_api_filters_provisional_and_future_prices_and_calculates_returns() throws Exception {
        String ticker = "990001";
        LocalDate previousDate = LocalDate.now().minusDays(2);
        LocalDate targetDate = LocalDate.now().minusDays(1);
        LocalDate futureDate = LocalDate.now().plusDays(1);
        Long stockId = jdbcTemplate.queryForObject("""
                INSERT INTO stock(market_code, ticker, name, market_type, currency, sector_code, sector_name, status)
                VALUES ('KOSPI', ?, 'API fixture', 'KOSPI', 'KRW', '0001', '전자', 'ACTIVE') RETURNING id
                """, Long.class, ticker);
        jdbcTemplate.update("""
                INSERT INTO stock_price_daily(stock_id, trade_date, quote_scope, open_price, high_price,
                    low_price, close_price, volume, trading_amount, data_status, source, collected_at)
                VALUES (?, ?, 'KRX', 109, 112, 108, 110, 1000, 110000, 'PROVISIONAL', 'KIS:TEST', now())
                """, stockId, targetDate);

        mockMvc.perform(get("/api/v1/stocks/{ticker}/prices/history", ticker)
                        .param("period", "1Y").param("frequency", "DAILY"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("S002"));

        jdbcTemplate.update("""
                INSERT INTO stock_price_daily(stock_id, trade_date, quote_scope, open_price, high_price,
                    low_price, close_price, volume, trading_amount, data_status, source, collected_at)
                VALUES (?, ?, 'KRX', 99, 102, 98, 100, 1000, 100000, 'FINAL', 'KIS:TEST', now())
                """, stockId, previousDate);
        jdbcTemplate.update("UPDATE stock_price_daily SET data_status = 'FINAL' WHERE stock_id = ? AND trade_date = ?",
                stockId, targetDate);
        jdbcTemplate.update("""
                INSERT INTO stock_price_daily(stock_id, trade_date, quote_scope, open_price, high_price,
                    low_price, close_price, volume, trading_amount, data_status, source, collected_at)
                VALUES (?, ?, 'KRX', 999, 1002, 998, 1001, 1000, 1001000, 'FINAL', 'KIS:TEST', now())
                """, stockId, futureDate);
        jdbcTemplate.update("""
                INSERT INTO stock_price_adjustment(stock_id, trade_date, quote_scope, adjustment_basis,
                    adjustment_as_of, adjusted_close, source, collected_at)
                VALUES (?, ?, 'KRX', 'PROVIDER_ADJUSTED', ?, 100, 'KIS:TEST', now()),
                       (?, ?, 'KRX', 'PROVIDER_ADJUSTED', ?, 110, 'KIS:TEST', now())
                """, stockId, previousDate, targetDate, stockId, targetDate, targetDate);

        long seriesId = canonicalKospiSeriesId();
        saveFinalIndexPrice(seriesId, previousDate, 2000);
        saveFinalIndexPrice(seriesId, targetDate, 2100);
        long sectorIndexId = jdbcTemplate.queryForObject("""
                INSERT INTO market_index(index_key, name, kind)
                VALUES ('API:SECTOR-101', 'Sector fixture', 'SECTOR') RETURNING id
                """, Long.class);
        long sectorSeriesId = jdbcTemplate.queryForObject("""
                INSERT INTO index_series(index_id, provider, provider_namespace, provider_series_code,
                    calendar_market_code, return_basis, dividend_basis, currency, is_canonical)
                VALUES (?, 'KIS', 'kis.api-test', '101', 'KOSPI', 'PRICE', 'NONE', 'KRW', true)
                RETURNING id
                """, Long.class, sectorIndexId);
        saveFinalIndexPrice(sectorSeriesId, targetDate, 1000);
        saveFinalWeather(seriesId, targetDate, 82);
        saveFinalWeather(sectorSeriesId, targetDate, 91);
        jdbcTemplate.update("""
                INSERT INTO stock_investor_trade_daily(stock_id, trade_date, quote_scope, investor_type,
                    net_quantity, net_amount, data_status, source, collected_at)
                VALUES (?, ?, 'KRX', 'INSTITUTION_TOTAL', 5, 5000, 'FINAL', 'KIS:TEST', now()),
                       (?, ?, 'KRX', 'FOREIGN', 7, 7000, 'FINAL', 'KIS:TEST', now())
                """, stockId, targetDate, stockId, targetDate);

        mockMvc.perform(get("/api/v1/market/indexes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.indexes[0].ticker").value("0001"))
                .andExpect(jsonPath("$.data.indexes[0].currentPrice").value(2100));
        mockMvc.perform(get("/api/v1/market-weather/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseDate").value(targetDate.toString()))
                .andExpect(jsonPath("$.data.weatherScore").value(91))
                .andExpect(jsonPath("$.data.weatherState").value("CLEAR"))
                .andExpect(jsonPath("$.data.topSectors[0].sectorCode").value("101"))
                .andExpect(jsonPath("$.data.bottomSectors[0].score").value(91));
        mockMvc.perform(get("/api/v1/stocks/ranking/supply").param("direction", "BUY").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effectiveDate").value(targetDate.toString()))
                .andExpect(jsonPath("$.data.institutionItems[0].ticker").value(ticker))
                .andExpect(jsonPath("$.data.foreignItems[0].netBuyingAmount").value(7000));

        String detailBody = mockMvc.perform(get("/api/v1/stocks/{ticker}", ticker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseDate").value(targetDate.toString()))
                .andReturn().getResponse().getContentAsString();
        JsonNode detail = objectMapper.readTree(detailBody).path("data");
        assertThat(detail.path("currentPrice").decimalValue()).isEqualByComparingTo("110");

        String chartBody = mockMvc.perform(get("/api/v1/stocks/{ticker}/prices/history", ticker)
                        .param("period", "1Y").param("frequency", "DAILY"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode prices = objectMapper.readTree(chartBody).path("data").path("prices");
        assertThat(prices).hasSize(2);
        assertThat(prices.get(1).path("date").asText()).isEqualTo(targetDate.toString());
        assertThat(prices.get(1).path("close").decimalValue()).isEqualByComparingTo("110");

        String returnsBody = mockMvc.perform(get("/api/v1/stocks/{ticker}/returns", ticker).param("period", "1Y"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode returns = objectMapper.readTree(returnsBody).path("data");
        assertThat(returns.path("stockReturnRate").decimalValue()).isEqualByComparingTo("10.0000");
        assertThat(returns.path("benchmarkReturnRate").decimalValue()).isEqualByComparingTo("5.0000");
    }

    @Test
    void authenticated_simulated_portfolio_uses_final_eod_price_and_persists_through_api() throws Exception {
        String ticker = "990002";
        LocalDate asOfDate = LocalDate.now().minusDays(1);
        Long stockId = jdbcTemplate.queryForObject("""
                INSERT INTO stock(market_code, ticker, name, market_type, currency, sector_code, sector_name, status)
                VALUES ('KOSPI', ?, 'Portfolio API fixture', 'KOSPI', 'KRW', '0001', '전자', 'ACTIVE') RETURNING id
                """, Long.class, ticker);
        jdbcTemplate.update("""
                INSERT INTO stock_price_daily(stock_id, trade_date, quote_scope, open_price, high_price,
                    low_price, close_price, volume, trading_amount, data_status, source, collected_at)
                VALUES (?, ?, 'KRX', 98, 102, 97, 100, 1000, 100000, 'FINAL', 'KIS:TEST', now())
                """, stockId, asOfDate);
        jdbcTemplate.update("""
                INSERT INTO stock_price_daily(stock_id, trade_date, quote_scope, open_price, high_price,
                    low_price, close_price, volume, trading_amount, data_status, source, collected_at)
                VALUES (?, ?, 'KRX', 88, 92, 87, 90, 1000, 90000, 'FINAL', 'KIS:TEST', now())
                """, stockId, asOfDate.minusDays(1));
        long benchmarkSeriesId = canonicalKospiSeriesId();
        saveFinalIndexPrice(benchmarkSeriesId, asOfDate.minusDays(1), 2000);
        saveFinalIndexPrice(benchmarkSeriesId, asOfDate, 2100);
        String accessToken = authUseCase.login(new LoginCommand(
                "canonical-portfolio@example.test", "canonical-portfolio", LoginType.KAKAO)).accessToken();

        String body = mockMvc.perform(post("/api/v1/portfolios/simulated")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Canonical API portfolio","description":"DB fixture",
                                 "totalAmount":10000,"items":[{"symbol":"990002","targetWeight":100}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.asOfDate").value(asOfDate.toString()))
                .andReturn().getResponse().getContentAsString();
        long portfolioId = objectMapper.readTree(body).path("data").path("portfolioId").asLong();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT quantity FROM portfolio_item WHERE portfolio_id = ?", java.math.BigDecimal.class, portfolioId))
                .isEqualByComparingTo("100.000000");
        String detailBody = mockMvc.perform(get("/api/v1/portfolios/{portfolioId}", portfolioId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Canonical API portfolio"))
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(detailBody).path("data").path("items").get(0).path("purchasePrice")
                .decimalValue()).isEqualByComparingTo("100");

        mockMvc.perform(get("/api/v1/portfolios/{portfolioId}/analysis/valuation", portfolioId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valuationStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.data.totalPurchaseAmount").value(10000))
                .andExpect(jsonPath("$.data.currentTotalValue").value(10000))
                .andExpect(jsonPath("$.data.dailyProfitLoss").value(1000))
                .andExpect(jsonPath("$.data.asOfDate").value(asOfDate.toString()));

        given(aiAdvisorUseCase.generateBacktestAdvice(any(), anyString(), anyString()))
                .willReturn("fixture advice");
        String backtestBody = mockMvc.perform(post("/api/v1/portfolios/{portfolioId}/analysis/backtest", portfolioId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"strategy":"LUMP_SUM","amount":10000,"primaryBenchmark":"KOSPI",
                                 "period":"1M","dividendReinvested":true,"rebalancingPeriod":"NONE"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode backtest = objectMapper.readTree(backtestBody).path("data");
        assertThat(backtest.path("primaryBenchmark").asText()).isEqualTo("KOSPI");
        assertThat(backtest.path("cagr").isNumber()).isTrue();
        assertThat(backtest.path("comparisons")).hasSize(1);
        assertThat(backtest.path("comparisons").get(0).path("totalReturn").decimalValue())
                .isEqualByComparingTo("5.0000");
        assertThat(backtest.path("aiComment").asText()).isEqualTo("fixture advice");
    }

    private Long canonicalKospiSeriesId() {
        Long indexId = jdbcTemplate.queryForObject("""
                INSERT INTO market_index(index_key, name, kind, public_benchmark_code)
                VALUES ('API:KOSPI', 'KOSPI fixture', 'MARKET', '0001')
                ON CONFLICT (public_benchmark_code) DO UPDATE SET name = EXCLUDED.name
                RETURNING id
                """, Long.class);
        return jdbcTemplate.queryForObject("""
                INSERT INTO index_series(index_id, provider, provider_namespace, provider_series_code,
                    calendar_market_code, return_basis, dividend_basis, currency, is_canonical)
                VALUES (?, 'KIS', 'kis.api-test', '0001', 'KOSPI', 'PRICE', 'NONE', 'KRW', true)
                ON CONFLICT (provider, provider_namespace, provider_series_code, return_basis, dividend_basis, currency)
                DO UPDATE SET is_canonical = true
                RETURNING id
                """, Long.class, indexId);
    }

    private void saveFinalIndexPrice(long seriesId, LocalDate date, int close) {
        jdbcTemplate.update("""
                INSERT INTO index_daily(series_id, trade_date, open_value, high_value, low_value, close_value,
                    price_status, price_source, collected_at)
                VALUES (?, ?, ?, ?, ?, ?, 'FINAL', 'KIS:TEST', now())
                ON CONFLICT (series_id, trade_date) DO UPDATE SET
                    open_value = EXCLUDED.open_value, high_value = EXCLUDED.high_value,
                    low_value = EXCLUDED.low_value, close_value = EXCLUDED.close_value,
                    price_status = 'FINAL'
                """, seriesId, date, close - 10, close + 10, close - 20, close);
    }

    private void saveFinalWeather(long seriesId, LocalDate date, int score) {
        jdbcTemplate.update("""
                INSERT INTO index_weather(series_id, trade_date, score, status, score_version, input_manifest,
                    calculated_at)
                VALUES (?, ?, ?, 'FINAL', 'fixture-v1', jsonb_build_object(
                    'source', 'index_daily', 'seriesId', CAST(? AS text), 'targetDate', CAST(? AS text),
                    'observations', jsonb_build_array(jsonb_build_object(
                        'date', CAST(? AS text), 'revision', 1))), now())
                """, seriesId, date, score, seriesId, date, date);
    }

    @Test
    void production_api_context_restarts_against_the_existing_schema_and_data() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Member member = Member.register("restart-" + suffix + "@example.test", "restart" + suffix, LoginType.KAKAO);
        Member savedMember = memberRepository.saveAndFlush(member);
        String description = "restart-description-" + suffix;
        portfolioRepository.saveAndFlush(Portfolio.create(savedMember.getId(), "restart portfolio", description));

        try (var restarted = new SpringApplicationBuilder(StockwellnessApiApplication.class)
                .profiles("prod")
                .run(restartArguments())) {
            JdbcTemplate restartedJdbc = new JdbcTemplate(restarted.getBean(DataSource.class));
            assertThat(restartedJdbc.queryForObject(
                    "select description from portfolio where member_id = ?", String.class, savedMember.getId()))
                    .isEqualTo(description);
            assertThat(restartedJdbc.queryForObject(
                    "select count(*) from information_schema.tables "
                            + "where table_schema='public' and table_type='BASE TABLE'",
                    Integer.class)).isEqualTo(37);
        }
    }

    private static String[] restartArguments() {
        return new String[] {
                "--DB_URL=" + postgres.getJdbcUrl(),
                "--DB_USERNAME=" + postgres.getUsername(),
                "--DB_PASSWORD=" + postgres.getPassword(),
                "--spring.datasource.driver-class-name=" + postgres.getDriverClassName(),
                "--spring.jpa.hibernate.ddl-auto=validate",
                "--spring.sql.init.mode=never",
                "--spring.kafka.listener.auto-startup=false",
                "--spring.kafka.admin.auto-create=false",
                "--spring.kafka.bootstrap-servers=localhost:9092",
                "--spring.cache.type=none",
                "--management.health.redis.enabled=false",
                "--management.health.kafka.enabled=false",
                "--server.port=0",
                "--KAKAO_CLIENT_ID=dummy-kakao-client-id",
                "--KAKAO_CLIENT_SECRET=dummy-kakao-client-secret",
                "--GOOGLE_CLIENT_ID=dummy-google-client-id",
                "--GOOGLE_CLIENT_SECRET=dummy-google-client-secret",
                "--OPENAI_API_KEY=dummy-openai-key",
                "--JWT_SECRET=dGhpcy1pcy10ZXN0LXNlY3JldC1rZXktZm9yLXN0b2Nrd2VsbG5lc3MtcHJvamVjdC13aGljaC1pcy1zYWZl",
                "--KIS_APP_KEY=dummy-kis-key",
                "--KIS_APP_SECRET=dummy-kis-secret",
                "--REDIRECT_URL=http://localhost:5173/auth/callback",
                "--SLACK_WEBHOOK_URL_API="
        };
    }

    static class DockerAvailable implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            return DockerClientFactory.instance().isDockerAvailable()
                    ? ConditionEvaluationResult.enabled("PostgreSQL Testcontainers available")
                    : ConditionEvaluationResult.disabled(
                            "PostgreSQL unverified: Docker daemon unavailable; no H2 fallback");
        }
    }
}
