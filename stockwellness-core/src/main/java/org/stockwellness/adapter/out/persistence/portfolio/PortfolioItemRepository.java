package org.stockwellness.adapter.out.persistence.portfolio;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.stockwellness.domain.portfolio.PortfolioItem;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {
    @Query("SELECT item FROM PortfolioItem item JOIN FETCH item.stock WHERE item.stock.ticker = :symbol")
    List<PortfolioItem> findAllBySymbol(@Param("symbol") String symbol);
}
