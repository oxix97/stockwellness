package org.stockwellness.adapter.out.persistence.portfolio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.portfolio.PortfolioItem;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {
    List<PortfolioItem> findAllBySymbol(String symbol);
}
