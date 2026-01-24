package org.stockwellness.application.port.out.portfolio;

import org.stockwellness.domain.portfolio.Portfolio;

public interface SavePortfolioPort {
    Portfolio savePortfolio(Portfolio portfolio);
}
