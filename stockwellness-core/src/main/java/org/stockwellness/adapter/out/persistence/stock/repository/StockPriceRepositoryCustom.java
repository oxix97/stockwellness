package org.stockwellness.adapter.out.persistence.stock.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.in.stock.result.StockSupplyRankingResult;
import org.stockwellness.application.port.out.stock.MarketBreadthItem;
import org.stockwellness.application.port.out.stock.MarketBreadthSnapshot;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.AlignmentStatus;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TradeDirection;

public interface StockPriceRepositoryCustom {

    Optional<StockPrice> findLatestPriceByName(String name);

    Map<String, BigDecimal> findLatestPricesByTickers(List<String> tickers);

    Map<String, List<StockPrice>> findRecentPricesBatch(List<String> tickers, int limit);

    List<StockPrice> findAllByDateWithStock(LocalDate baseDate);

    List<StockPriceResult> findAllByTickerAndPeriod(String ticker, LocalDate start, LocalDate end);

    List<StockPriceResult> findAllByTickerAndYear(String ticker, int year);

    List<StockPrice> findRecentPricesByStocks(List<Stock> stocks, LocalDate date, int limit);

    Map<Long, LocalDate> findLatestBaseDatesByStocks(List<Stock> stocks);

    Optional<LocalDate> findLatestDate();

    Optional<LocalDate> findLatestDateOnOrBefore(LocalDate date);

    Optional<LocalDate> findLatestInvestorTradeDate();

    MarketBreadthSnapshot summarizeBreadthByDate(LocalDate baseDate);

    List<StockPrice> findFilteredStocksByIndicators(
            LocalDate baseDate,
            AlignmentStatus alignment,
            BigDecimal rsiLow,
            BigDecimal rsiHigh,
            Boolean isGoldenCross
    );

    List<StockSupplyRankingResult> findTopInstitutionStocksBySupply(LocalDate date, TradeDirection direction, int limit);

    List<StockSupplyRankingResult> findTopForeignStocksBySupply(LocalDate date, TradeDirection direction, int limit);

    List<MarketBreadthItem> findAllBreadthItemsByDate(LocalDate baseDate);

    List<StockPrice> findByStockInAndIdBaseDate(List<Stock> stocks, LocalDate baseDate);
}
