package org.stockwellness.application.port.in.portfolio;

import org.stockwellness.application.port.in.portfolio.result.PortfolioValuationResult;

public interface PortfolioValuationUseCase {
    PortfolioValuationResult getValuation(Long memberId, Long portfolioId);
}
