package org.stockwellness.application.port.in.portfolio;

import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioResponse;
import org.stockwellness.application.port.in.portfolio.command.CreatePortfolioCommand;
import org.stockwellness.application.port.in.portfolio.command.UpdatePortfolioCommand;

import java.util.List;

public interface PortfolioUseCase {
    Long createPortfolio(CreatePortfolioCommand command);
    void updatePortfolio(UpdatePortfolioCommand command);
    void deletePortfolio(Long memberId, Long portfolioId);
    PortfolioResponse getPortfolio(Long memberId, Long portfolioId);
    List<PortfolioResponse> getMyPortfolios(Long memberId);
}
