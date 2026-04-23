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

    /**
     * stockId를 가지고 가장 최근의 날짜 기준 120개 조회
     */
    List<StockPrice> findRecent120Prices(Long stockId);
    /**
     * 특정 날짜 기준 수급(외인/기관) 상위 종목 목록을 조회합니다.
     */
    List<StockSupplyRankingResult> findTopInstitutionStocksBySupply(
            LocalDate date,
            TradeDirection direction,
            int limit
    );

    List<StockSupplyRankingResult> findTopForeignStocksBySupply(
            LocalDate date,
            TradeDirection direction,
            int limit
    );

    /**
     * 지정된 날짜의 모든 종목 시세 데이터를 조회합니다.
     */
    List<StockPrice> findAllByDate(LocalDate date);

    /**
     * 시장 등락 분포 계산을 위한 최소 데이터를 조회합니다.
     */
    List<MarketBreadthItem> findAllBreadthItemsByDate(LocalDate date);

    Optional<StockPrice> findLateststockPrice(String isinCode);

    /**
     * 특정 종목의 가장 최신 시세 데이터를 조회합니다. (티커 기준)
     */
    Optional<StockPrice> findLatestByTicker(String ticker);

    /**
     * 여러 종목의 가장 최신 시세 데이터를 한 번에 조회합니다. (티커 기준)
     */
    Map<String, BigDecimal> findAllLatestByTickers(List<String> tickers);

    Map<String, List<StockPrice>> loadRecentHistoriesBatch(List<String> isinCodes, int limit);

    /**
     * 티커와 기간을 기준으로 일봉 데이터를 로드합니다.
     */
    List<StockPriceResult> loadPricesByTicker(String ticker, LocalDate start, LocalDate end);

    /**
     * 여러 티커와 기간을 기준으로 일봉 데이터를 로드합니다. (벌크 조회용)
     */
    Map<String, List<StockPriceResult>> loadPricesByTickers(List<String> tickers, LocalDate start, LocalDate end);


    /**
     * 여러 종목의 마지막 저장일들을 한 번에 조회합니다.
     */
    Map<Long, LocalDate> findLatestBaseDatesByStocks(List<Stock> stocks);


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

    void saveAll(List<StockPrice> stockPrices);
}
