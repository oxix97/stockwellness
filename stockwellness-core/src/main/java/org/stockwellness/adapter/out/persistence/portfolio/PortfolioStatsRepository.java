package org.stockwellness.adapter.out.persistence.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.portfolio.PortfolioStats;

import java.util.Optional;

public interface PortfolioStatsRepository extends JpaRepository<PortfolioStats, Long> {
    Optional<PortfolioStats> findByPortfolioId(Long portfolioId);
}
