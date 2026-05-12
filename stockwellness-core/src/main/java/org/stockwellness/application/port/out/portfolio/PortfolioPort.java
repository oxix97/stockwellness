package org.stockwellness.application.port.out.portfolio;

import java.util.List;
import java.util.Optional;

import org.stockwellness.domain.portfolio.Portfolio;

public interface PortfolioPort {
    // Load methods
    Optional<Portfolio> findById(Long id);
    Optional<Portfolio> loadPortfolio(Long id, Long memberId);
    List<Portfolio> loadAllPortfolios(Long memberId);
    List<Portfolio> loadAllWithItems(List<Long> ids);
    List<Long> findPortfolioIdsBySymbols(List<String> symbols);
    List<Long> findAllIds(int offset, int limit);
    boolean existsPortfolioName(Long memberId, String name);

    // Save methods
    Portfolio savePortfolio(Portfolio portfolio);

    // Delete methods
    void deletePortfolio(Long id);
}
