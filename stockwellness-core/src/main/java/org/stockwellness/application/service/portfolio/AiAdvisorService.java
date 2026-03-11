package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.portfolio.AdvicePortfolioUseCase;
import org.stockwellness.application.port.in.portfolio.result.AdviceResponse;
import org.stockwellness.application.port.out.portfolio.AiAdvisorPort;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.service.portfolio.internal.AdvisorAiDataLoader;
import org.stockwellness.domain.portfolio.exception.PortfolioAccessDeniedException;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AiAdvisorService implements AdvicePortfolioUseCase {

    private final PortfolioPort portfolioPort;
    private final AdvisorAiDataLoader dataLoader;
    private final AiAdvisorPort aiAdvisorPort;

    @Override
    @Transactional(readOnly = true)
    public AdviceResponse getAdvice(Long memberId, Long portfolioId) {
        // 1. 소유권 확인
        portfolioPort.loadPortfolio(portfolioId, memberId)
                .orElseThrow(() -> {
                    if (portfolioPort.findById(portfolioId).isPresent()) {
                        throw new PortfolioAccessDeniedException();
                    }
                    return new PortfolioNotFoundException();
                });

        // 2. 데이터 로딩
        var context = dataLoader.loadContext(portfolioId);

        // 3. AI 조언 요청
        var aiResult = aiAdvisorPort.getRebalancingAdvice(context);

        // 4. 결과 변환 (AdviceResponse)
        return new AdviceResponse(
                aiResult.adviceContent(),
                aiResult.primaryAction(),
                LocalDateTime.now()
        );
    }
}
