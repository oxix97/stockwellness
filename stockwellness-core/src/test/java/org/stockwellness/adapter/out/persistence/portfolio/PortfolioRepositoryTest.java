package org.stockwellness.adapter.out.persistence.portfolio;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.stockwellness.TestCoreApplication;
import org.stockwellness.config.JpaConfig;
import org.stockwellness.config.QueryDslConfig;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.advisor.AdviceAction;
import org.stockwellness.domain.portfolio.advisor.AdvisorReport;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Import({QueryDslConfig.class, JpaConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PortfolioRepository 통합 테스트")
class PortfolioRepositoryTest {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private AdvisorReportRepository advisorReportRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("포트폴리오 조회 시 아이템과 함께 어드바이저 보고서도 Fetch Join으로 조회한다")
    void findWithItems_with_advisor_reports_success() {
        // given
        Portfolio portfolio = Portfolio.create(1L, "테스트 포트폴리오", "설명");
        portfolioRepository.save(portfolio);
        
        AdvisorReport report = AdvisorReport.create(portfolio, "AI 조언", AdviceAction.REBALANCE);
        advisorReportRepository.save(report);

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Portfolio> result = portfolioRepository.findWithItems(portfolio.getId(), 1L);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAdvisorReports()).hasSize(1);
    }

    @Test
    @DisplayName("가장 최근의 어드바이저 보고서를 조회한다")
    void loadLatestReport_success() throws InterruptedException {
        // given
        Portfolio portfolio = Portfolio.create(1L, "테스트 포트폴리오", "설명");
        portfolioRepository.save(portfolio);

        AdvisorReport oldReport = AdvisorReport.create(portfolio, "옛날 조언", AdviceAction.REBALANCE);
        advisorReportRepository.save(oldReport);
        
        entityManager.flush();
        Thread.sleep(100);

        AdvisorReport newReport = AdvisorReport.create(portfolio, "새 조언", AdviceAction.RISK_MANAGEMENT);
        advisorReportRepository.save(newReport);

        entityManager.flush();
        entityManager.clear();

        // when
        Optional<AdvisorReport> latest = advisorReportRepository.findFirstByPortfolioIdOrderByCreatedAtDesc(portfolio.getId());

        // then
        assertThat(latest).isPresent();
        assertThat(latest.get().getContent()).isEqualTo("새 조언");
    }
}
