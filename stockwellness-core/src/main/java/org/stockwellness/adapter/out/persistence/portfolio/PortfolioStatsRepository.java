package org.stockwellness.adapter.out.persistence.portfolio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.portfolio.PortfolioStats;

public interface PortfolioStatsRepository extends JpaRepository<PortfolioStats, Long> {
    Optional<PortfolioStats> findByPortfolioId(Long portfolioId);
}
