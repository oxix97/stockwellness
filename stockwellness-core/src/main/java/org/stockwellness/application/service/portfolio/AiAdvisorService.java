package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.portfolio.AiAdvisorUseCase;
import org.stockwellness.application.port.in.portfolio.result.AdviceResponse;
import org.stockwellness.application.port.out.portfolio.AiAdviceProviderPort;
import org.stockwellness.application.port.out.portfolio.LoadAdvisorPort;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.service.portfolio.internal.AdvisorAiDataLoader;
import org.stockwellness.domain.portfolio.exception.PortfolioAccessDeniedException;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiAdvisorService implements AiAdvisorUseCase {

    private final PortfolioPort portfolioPort;
    private final AdvisorAiDataLoader dataLoader;
    private final AiAdviceProviderPort aiAdviceProviderPort;
    private final LoadAdvisorPort loadAdvisorPort;

    @Override
    @Transactional
    public AdviceResponse getNewAdvice(Long memberId, Long portfolioId) {
        validateOwnership(memberId, portfolioId);

        var context = dataLoader.loadContext(portfolioId);
        var aiResult = aiAdviceProviderPort.getRebalancingAdvice(context);

        return new AdviceResponse(
                aiResult.adviceContent(),
                aiResult.primaryAction(),
                LocalDateTime.now()
        );
    }

    @Override
    public AdviceResponse getLatestAdvice(Long memberId, Long portfolioId) {
        validateOwnership(memberId, portfolioId);

        return loadAdvisorPort.loadLatestReport(portfolioId)
                .map(report -> new AdviceResponse(
                        report.getContent(),
                        report.getAction(),
                        report.getCreatedAt()
                ))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validateOwnership(Long memberId, Long portfolioId) {
        portfolioPort.loadPortfolio(portfolioId, memberId)
                .orElseThrow(() -> {
                    if (portfolioPort.findById(portfolioId).isPresent()) {
                        throw new PortfolioAccessDeniedException();
                    }
                    return new PortfolioNotFoundException();
                });
    }
}
