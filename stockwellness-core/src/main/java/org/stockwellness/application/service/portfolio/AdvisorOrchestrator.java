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

    private static final int BATCH_SIZE = 100;

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
        log.info("📢 Starting AI Advisor Orchestration in batches (size: {})", BATCH_SIZE);

        int offset = 0;
        int totalProcessed = 0;

        while (true) {
            List<Long> portfolioIds = portfolioPort.findAllIds(offset, BATCH_SIZE);
            if (portfolioIds.isEmpty()) {
                break;
            }

            log.info("Processing batch: offset={}, size={}", offset, portfolioIds.size());

            for (Long id : portfolioIds) {
                try {
                    generateAndSaveAdvice(id);
                    totalProcessed++;
                } catch (Exception e) {
                    log.error("⚠️ Error processing portfolio {}: {}", id, e.getMessage());
                }
            }

            offset += BATCH_SIZE;
        }

        log.info("✅ Completed AI Advisor Orchestration. Total processed: {}", totalProcessed);
    }
}
