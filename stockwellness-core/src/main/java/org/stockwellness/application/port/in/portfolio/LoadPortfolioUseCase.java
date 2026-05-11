package org.stockwellness.application.port.in.portfolio;

import java.util.List;

import org.stockwellness.application.port.in.portfolio.dto.PortfolioResponse;

public interface LoadPortfolioUseCase {
    PortfolioResponse getPortfolio(Long memberId, Long portfolioId);
    List<PortfolioResponse> getMyPortfolios(Long memberId);
}
