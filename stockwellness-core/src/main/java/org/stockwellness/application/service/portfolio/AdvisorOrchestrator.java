package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.out.portfolio.AiAdvisorPort;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.portfolio.SaveAdvisorPort;
import org.stockwellness.application.service.portfolio.internal.AdvisorAiDataLoader;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.advisor.AdvisorReport;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvisorOrchestrator {

    private final PortfolioPort portfolioPort;
    private final AdvisorAiDataLoader dataLoader;
    private final AiAdvisorPort aiAdvisorPort;
    private final SaveAdvisorPort saveAdvisorPort;

    /**
     * 특정 포트폴리오에 대해 AI 조언을 생성하고 저장한다.
     */
    @Transactional
    public void generateAndSaveAdvice(Long portfolioId) {
        Portfolio portfolio = portfolioPort.findById(portfolioId)
                .orElseThrow(); // TODO: Custom Exception

        log.info("🚀 Generating AI advice for portfolio: {} ({})", portfolio.getName(), portfolioId);

        try {
            var context = dataLoader.loadContext(portfolioId);
            var aiResult = aiAdvisorPort.getRebalancingAdvice(context);

            AdvisorReport report = AdvisorReport.create(
                    portfolio,
                    aiResult.adviceContent(),
                    aiResult.primaryAction()
            );

            saveAdvisorPort.saveReport(report);
            log.info("✅ Successfully saved AI advice for portfolio: {}", portfolioId);
        } catch (Exception e) {
            log.error("❌ Failed to generate AI advice for portfolio {}: {}", portfolioId, e.getMessage());
        }
    }

    /**
     * 모든 사용자의 포트폴리오에 대해 AI 조언을 생성한다. (스케줄러용)
     */
    public void runAllPortfolios() {
        // TODO: 페이징 처리 필요할 수 있음
        List<Portfolio> allPortfolios = portfolioPort.loadAllPortfolios(null); // 모든 회원 대상 (null 처리 정책 확인 필요)
        log.info("📢 Starting AI Advisor Orchestration for {} portfolios", allPortfolios.size());

        allPortfolios.forEach(p -> {
            try {
                generateAndSaveAdvice(p.getId());
            } catch (Exception e) {
                log.error("Error processing portfolio {}: {}", p.getId(), e.getMessage());
            }
        });
    }
}
