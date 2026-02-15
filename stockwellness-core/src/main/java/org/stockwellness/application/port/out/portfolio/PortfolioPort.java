package org.stockwellness.application.port.out.portfolio;

import org.stockwellness.domain.portfolio.Portfolio;

import java.util.List;
import java.util.Optional;

public interface PortfolioPort {
    // Load methods
    Optional<Portfolio> findById(Long id);
    Optional<Portfolio> loadPortfolio(Long id, Long memberId);
    List<Portfolio> loadAllPortfolios(Long memberId);
    boolean existsPortfolioName(Long memberId, String name);

    // Save methods
    Portfolio savePortfolio(Portfolio portfolio);

    // Delete methods
    void deletePortfolio(Long id);
}
