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
import java.util.Collection;
import java.util.List;

public interface StockPriceRepository extends JpaRepository<StockPrice, StockPriceId>, StockPriceRepositoryCustom {
    @Query("SELECT s.closePrice FROM StockPrice s WHERE s.stock = :stock AND s.id.baseDate < :date ORDER BY s.id.baseDate DESC")
    List<BigDecimal> findRecentClosingPrices(@Param("stock") Stock stock, @Param("date") LocalDate date, Pageable pageable);

    @Query("SELECT MAX(s.id.baseDate) FROM StockPrice s WHERE s.stock = :stock")
    LocalDate findLatestBaseDate(@Param("stock") Stock stock);

    @Query(value = "SELECT * FROM stock_price WHERE base_date = CAST(:baseDate AS date)", nativeQuery = true)
    List<StockPrice> findAllByIdBaseDate(@Param("baseDate") LocalDate baseDate);

    List<StockPrice> findByStockInAndIdBaseDate(Collection<Stock> stocks, LocalDate baseDate);

    @Query("SELECT s FROM StockPrice s WHERE s.id.stockId = :stockId ORDER BY s.id.baseDate ASC")
    List<StockPrice> findByStockIdOrderByBaseDateAsc(@Param("stockId") Long stockId);

    @Query("SELECT s FROM StockPrice s JOIN FETCH s.stock WHERE s.id.baseDate BETWEEN :startDate AND :endDate AND (s.closePrice IS NULL OR s.closePrice <= 0)")
    List<StockPrice> findInvalidPrices(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM StockPrice s WHERE s.stock.ticker IN :tickers AND s.id.baseDate BETWEEN :startDate AND :endDate ORDER BY s.id.baseDate ASC")
    List<StockPrice> findByStockTickerInAndIdBaseDateBetween(@Param("tickers") Collection<String> tickers, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM StockPrice s WHERE s.stock.ticker = :ticker AND s.id.baseDate <= :date ORDER BY s.id.baseDate DESC")
    List<StockPrice> findRecentPrices(@Param("ticker") String ticker, @Param("date") LocalDate date, Pageable pageable);

    @Query("SELECT s FROM StockPrice s WHERE s.stock.ticker IN :tickers AND s.id.baseDate <= :date ORDER BY s.id.baseDate DESC")
    List<StockPrice> findRecentPricesByTickers(@Param("tickers") Collection<String> tickers, @Param("date") LocalDate date);
}
