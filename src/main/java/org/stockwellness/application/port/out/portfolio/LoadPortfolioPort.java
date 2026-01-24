package org.stockwellness.application.port.out.portfolio;

import org.stockwellness.domain.portfolio.Portfolio;

import java.util.List;
import java.util.Optional;

public interface LoadPortfolioPort {
    Optional<Portfolio> loadPortfolio(Long id, Long memberId);
    List<Portfolio> loadAllPortfolios(Long memberId);
    boolean existsPortfolioName(Long memberId, String name);
}
