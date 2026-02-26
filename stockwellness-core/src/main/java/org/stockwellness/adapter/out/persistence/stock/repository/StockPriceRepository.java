package org.stockwellness.adapter.out.persistence.stock.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.StockPriceId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface StockPriceRepository extends JpaRepository<StockPrice, StockPriceId>, StockPriceRepositoryCustom {
    @Query("SELECT s.closePrice FROM StockPrice s WHERE s.stock = :stock AND s.id.baseDate < :date ORDER BY s.id.baseDate DESC")
    List<BigDecimal> findRecentClosingPrices(@Param("stock") Stock stock, @Param("date") LocalDate date, Pageable pageable);

    @Query("SELECT MAX(s.id.baseDate) FROM StockPrice s WHERE s.stock = :stock")
    LocalDate findLatestBaseDate(@Param("stock") Stock stock);
}
