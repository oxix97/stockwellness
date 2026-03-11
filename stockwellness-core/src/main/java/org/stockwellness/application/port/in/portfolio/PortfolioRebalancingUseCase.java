package org.stockwellness.application.port.in.portfolio;

import org.stockwellness.application.port.in.portfolio.result.PortfolioRebalancingResult;

public interface PortfolioRebalancingUseCase {
    PortfolioRebalancingResult getRebalancingGuide(Long memberId, Long portfolioId);
}
