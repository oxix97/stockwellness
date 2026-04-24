package org.stockwellness.application.port.out.stock;

import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.in.stock.result.StockSupplyRankingResult;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TradeDirection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StockPricePort {

    StockPrice save(StockPrice stockPrice);

    void saveAll(List<StockPrice> stockPrices);

    List<StockSupplyRankingResult> findTopInstitutionStocksBySupply(LocalDate date, TradeDirection direction, int limit);

    List<StockSupplyRankingResult> findTopForeignStocksBySupply(LocalDate date, TradeDirection direction, int limit);

    /**
     * 여러 종목의 과거 시세 엔티티 리스트를 한 번에 조회합니다. (지표 계산용 날짜 포함)
     */
    Map<Long, List<StockPrice>> findRecentPricesWithDateByStocks(List<Stock> stocks, LocalDate date, int limit);

    /**
     * 특정 종목의 과거 시세 엔티티 리스트를 조회합니다. (지표 계산용 날짜 포함)
     */
    List<StockPrice> findRecentPricesWithDateByStock(Stock stock, LocalDate date, int limit);

    Optional<LocalDate> findLatestDateOnOrBefore(LocalDate date);

    Optional<LocalDate> findLatestInvestorTradeDate();

    boolean existsByBaseDate(LocalDate date);

    /**
     * 여러 종목의 최신 저장일 목록을 한 번에 조회합니다.
     */
    Map<Long, LocalDate> findLatestBaseDatesByStocks(List<Stock> stocks);

    List<StockPrice> findAllByDate(LocalDate date);

    /**
     * 시장 등락 분포 계산을 위한 최소 데이터를 조회합니다.
     */
    List<MarketBreadthItem> findAllBreadthItemsByDate(LocalDate date);

    /**
     * 특정 날짜의 시장 등락 분포(Market Breadth)를 집계합니다.
     */
    MarketBreadthSnapshot summarizeBreadthByDate(LocalDate date);

    Optional<StockPrice> findLateststockPrice(String isinCode);

    /**
     * 특정 종목의 가장 최신 시세 데이터를 조회합니다. (티커 기준)
     */
    Optional<StockPrice> findLatestByTicker(String ticker);

    /**
     * 여러 티커의 최신 종가를 한 번에 조회합니다.
     */
    Map<String, BigDecimal> findAllLatestByTickers(List<String> tickers);

    /**
     * 여러 종목의 최근 시세 이력을 일괄 로드합니다.
     */
    Map<String, List<StockPrice>> loadRecentHistoriesBatch(List<String> isinCodes, int limit);

    /**
     * 특정 종목의 시세 데이터를 로드합니다. (캐시 우선 활용)
     */
    List<StockPriceResult> loadPricesByTicker(String ticker, LocalDate start, LocalDate end);

    /**
     * 여러 종목의 시세 데이터를 로드합니다.
     */
    Map<String, List<StockPriceResult>> loadPricesByTickers(List<String> tickers, LocalDate start, LocalDate end);
}
