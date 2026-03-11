package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.out.portfolio.AiAdviceProviderPort;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.portfolio.SaveAdvisorPort;
import org.stockwellness.application.service.portfolio.internal.AdvisorAiDataLoader;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.advisor.AdvisorReport;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvisorOrchestrator {

    private final PortfolioPort portfolioPort;
    private final AdvisorAiDataLoader dataLoader;
    private final AiAdviceProviderPort aiAdviceProviderPort;
    private final SaveAdvisorPort saveAdvisorPort;

    /**
     * 특정 포트폴리오에 대해 AI 조언을 생성하고 저장한다.
     */
    @Transactional
    public void generateAndSaveAdvice(Long portfolioId) {
        Portfolio portfolio = portfolioPort.findById(portfolioId)
                .orElseThrow(PortfolioNotFoundException::new);

        log.info("🚀 Generating AI advice for portfolio: {} ({})", portfolio.getName(), portfolioId);

        try {
            var context = dataLoader.loadContext(portfolioId);
            var aiResult = aiAdviceProviderPort.getRebalancingAdvice(context);

            AdvisorReport report = AdvisorReport.create(
                    portfolio,
                    aiResult.adviceContent(),
                    aiResult.primaryAction()
            );

            saveAdvisorPort.saveReport(report);
            log.info("✅ Successfully saved AI advice for portfolio: {}", portfolioId);
        } catch (Exception e) {
            log.error("❌ Failed to generate AI advice for portfolio {}: {}", portfolioId, e.getMessage());
            throw e; // Rethrow to let the caller (scheduler) know it failed
        }
    }

    /**
     * 모든 사용자의 포트폴리오에 대해 AI 조언을 생성한다. (스케줄러용)
     */
    public void runAllPortfolios() {
        // TODO: 페이징 처리 필요 (포트폴리오가 많아질 경우)
        List<Portfolio> allPortfolios = portfolioPort.loadAllPortfolios(null);
        log.info("📢 Starting AI Advisor Orchestration for {} portfolios", allPortfolios.size());

        allPortfolios.forEach(p -> {
            try {
                generateAndSaveAdvice(p.getId());
            } catch (Exception e) {
                // Individual failures shouldn't stop the whole process
                log.error("⚠️ Error processing portfolio {}: {}", p.getId(), e.getMessage());
            }
        });
    }
}
