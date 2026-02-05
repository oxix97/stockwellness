package org.stockwellness.application.port.in.portfolio;

import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioResponse;

import java.util.List;

public interface LoadPortfolioUseCase {
    PortfolioResponse getPortfolio(Long memberId, Long portfolioId);
    List<PortfolioResponse> getMyPortfolios(Long memberId);
}
