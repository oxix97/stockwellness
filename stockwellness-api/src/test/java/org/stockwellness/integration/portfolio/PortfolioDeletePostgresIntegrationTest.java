package org.stockwellness.integration.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.member.MemberRepository;
import org.stockwellness.adapter.out.persistence.portfolio.AdvisorReportRepository;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioRepository;
import org.stockwellness.application.port.in.auth.command.LoginCommand;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioCreateRequest;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioItemRequest;
import org.stockwellness.application.service.portfolio.PortfolioStatBatchService;
import org.stockwellness.application.service.portfolio.internal.BacktestResult;
import org.stockwellness.application.service.portfolio.internal.SimulationData;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.advisor.AdviceAction;
import org.stockwellness.domain.portfolio.advisor.AdvisorReport;
import org.stockwellness.domain.shared.Email;
import org.stockwellness.integration.common.BaseIntegrationTest;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.stockwellness.domain.portfolio.AssetType.CASH;

@ExtendWith(PortfolioDeletePostgresIntegrationTest.DockerAvailable.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.kafka.admin.auto-create=false")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PortfolioDeletePostgresIntegrationTest extends BaseIntegrationTest {

    private static PostgreSQLContainer<?> postgres;

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        postgres = new PostgreSQLContainer<>("postgres:15-alpine");
        postgres.start();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @AfterAll
    static void stopPostgres() {
        if (postgres != null) postgres.stop();
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private TestRestTemplate http;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PortfolioRepository portfolioRepository;
    @Autowired private AdvisorReportRepository advisorReportRepository;
    @Autowired private PortfolioStatBatchService portfolioStatBatchService;

    @Test
    void authenticatedDeleteRemovesOnlyTheOwnedPortfolioAndItsDependents() throws Exception {
        assertThat(jdbc.queryForObject("select version()", String.class)).startsWith("PostgreSQL");
        String targetToken = loginAndGetToken("issue331-target@example.com", "issue331-target");
        authUseCase.login(new LoginCommand("issue331-other@example.com", "issue331-other", LoginType.KAKAO));
        Long otherMemberId = memberRepository.findByEmailAndLoginType(
                new Email("issue331-other@example.com"), LoginType.KAKAO).orElseThrow().getId();

        HttpHeaders targetHeaders = new HttpHeaders();
        targetHeaders.setBearerAuth(targetToken);
        targetHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> created = http.postForEntity("/api/v1/portfolios", new HttpEntity<>(
                objectMapper.writeValueAsString(new PortfolioCreateRequest(
                        "issue331 target", "delete regression",
                        List.of(new PortfolioItemRequest("CASH", BigDecimal.ONE, BigDecimal.ONE,
                                "KRW", CASH, new BigDecimal("100"))))), targetHeaders), String.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        Long targetId = objectMapper.readTree(created.getBody()).path("data").asLong();
        generateStatsAndReport(targetId);

        Portfolio other = Portfolio.create(otherMemberId, "other member", "must survive");
        other.updateItems(List.of(PortfolioItem.createCash(BigDecimal.ONE, "KRW",
                new BigDecimal("100"), LocalDate.now())));
        other = portfolioRepository.saveAndFlush(other);
        Long otherId = other.getId();
        generateStatsAndReport(otherId);

        assertThat(count("portfolio_item", targetId)).isEqualTo(1);
        assertThat(count("portfolio_stats", targetId)).isEqualTo(1);
        assertThat(count("portfolio_ai_report", targetId)).isEqualTo(1);
        assertThat(count("portfolio_item", otherId)).isEqualTo(1);
        assertThat(count("portfolio_stats", otherId)).isEqualTo(1);
        assertThat(count("portfolio_ai_report", otherId)).isEqualTo(1);

        ResponseEntity<String> deleted = http.exchange("/api/v1/portfolios/{portfolioId}", HttpMethod.DELETE,
                new HttpEntity<>(targetHeaders), String.class, targetId);
        assertThat(deleted.getStatusCode().value()).isEqualTo(200);
        assertThat(objectMapper.readTree(deleted.getBody()).path("success").asBoolean()).isTrue();
        assertThat(objectMapper.readTree(deleted.getBody()).path("code").asText()).isEqualTo("S000");

        assertThat(count("portfolio", targetId)).isZero();
        assertThat(count("portfolio_item", targetId)).isZero();
        assertThat(count("portfolio_stats", targetId)).isZero();
        assertThat(count("portfolio_ai_report", targetId)).isZero();
        assertThat(count("portfolio", otherId)).isEqualTo(1);
        assertThat(count("portfolio_item", otherId)).isEqualTo(1);
        assertThat(count("portfolio_stats", otherId)).isEqualTo(1);
        assertThat(count("portfolio_ai_report", otherId)).isEqualTo(1);
    }

    private void generateStatsAndReport(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findAllWithItemsByIdIn(List.of(portfolioId)).getFirst();
        portfolioStatBatchService.saveStatsAndEvent(portfolio, BacktestResult.empty(),
                new SimulationData(Map.of(), Map.of()), LocalDate.now());
        advisorReportRepository.saveAndFlush(AdvisorReport.create(
                portfolioId, "test-model", "test-prompt", "test-hash", "test report", AdviceAction.HOLD));
    }

    private int count(String table, Long portfolioId) {
        return jdbc.queryForObject("select count(*) from " + table + " where "
                + (table.equals("portfolio") ? "id" : "portfolio_id") + " = ?", Integer.class, portfolioId);
    }

    static class DockerAvailable implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            return DockerClientFactory.instance().isDockerAvailable()
                    ? ConditionEvaluationResult.enabled("PostgreSQL Testcontainers available")
                    : ConditionEvaluationResult.disabled("PostgreSQL unverified: Docker daemon unavailable");
        }
    }
}
