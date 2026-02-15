package org.stockwellness.application.port.in.portfolio;

import org.stockwellness.application.port.in.portfolio.command.CreatePortfolioCommand;
import org.stockwellness.application.port.in.portfolio.command.UpdatePortfolioCommand;

public interface ManagePortfolioUseCase {
    Long createPortfolio(CreatePortfolioCommand command);
    void updatePortfolio(UpdatePortfolioCommand command);
    void deletePortfolio(Long memberId, Long portfolioId);
}
