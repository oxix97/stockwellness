package org.stockwellness.adapter.out.persistence.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.portfolio.PortfolioItem;

import java.util.List;

public interface PortfolioItemJpaRepository extends JpaRepository<PortfolioItem, Long> {
    List<PortfolioItem> findAllByIsinCode(String isinCode);
}
