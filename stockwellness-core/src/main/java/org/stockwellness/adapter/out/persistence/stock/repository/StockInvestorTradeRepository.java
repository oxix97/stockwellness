package org.stockwellness.adapter.out.persistence.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.stock.price.StockInvestorTrade;
import org.stockwellness.domain.stock.price.StockPriceId;

public interface StockInvestorTradeRepository extends JpaRepository<StockInvestorTrade, StockPriceId> {
}
