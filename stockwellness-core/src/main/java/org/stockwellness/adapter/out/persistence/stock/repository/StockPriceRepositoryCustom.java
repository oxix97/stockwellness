package org.stockwellness.adapter.out.persistence.stock.repository;

import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.in.stock.result.StockSupplyRankingResult;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.AlignmentStatus;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TradeDirection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StockPriceRepositoryCustom {

    Optional<StockPrice> findLatestPriceByName(String name);
    /**
     * 특정 날짜 기준 수급(외인/기관) 순매수량/순매도량 상위 종목 목록을 조회합니다.
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
     * 전체 종목 중 가장 최신 저장일 조회를 조회합니다.
     */
    Optional<LocalDate> findLatestDate();

    /**
     * 지정일 이전 또는 당일 기준 가장 최신 저장일을 조회합니다.
     */
    Optional<LocalDate> findLatestDateOnOrBefore(LocalDate date);

    /**
     * stock_investor_trade 기준 가장 최신 적재일을 조회합니다.
     */
    Optional<LocalDate> findLatestInvestorTradeDate();

    /**
     * 기술적 지표를 기반으로 종목을 필터링하여 조회합니다.
     */
    List<StockPrice> findFilteredStocksByIndicators(
            LocalDate baseDate,
            AlignmentStatus alignment,
            BigDecimal rsiLow,
            BigDecimal rsiHigh,
            Boolean isGoldenCross
    );

    /**
     * 특정 종목들의 지정된 날짜 시세 데이터를 조회합니다. (Fetch Join 적용)
     */
    List<StockPrice> findByStockInAndIdBaseDate(List<Stock> stocks, LocalDate baseDate);

    /**
     * 지정된 날짜의 모든 종목 시세 데이터를 fetch join으로 조회합니다.
     */
    List<StockPrice> findAllByDateWithStock(LocalDate baseDate);


    /**
     * 특정 티커의 연도별 시세 데이터를 조회합니다. (캐싱 용도)
     */
    List<StockPriceResult> findAllByTickerAndYear(String ticker, int year);

    /**
     * 특정 티커의 기간별 시세 데이터를 조회합니다.
     */
    List<StockPriceResult> findAllByTickerAndPeriod(String ticker, LocalDate start, LocalDate end);

    /**
     * 여러 종목의 마지막 저장일들을 한 번에 조회합니다. (QueryDSL)
     */
    Map<Long, LocalDate> findLatestBaseDatesByStocks(List<Stock> stocks);

    /**
     * 여러 종목의 최근 가격 데이터를 한 번에 조회합니다. (QueryDSL)
     */
    List<StockPrice> findRecentPricesByStocks(List<Stock> stocks, LocalDate date, int limit);

    /**
     * 여러 티커의 가장 최신 종가 데이터를 한 번에 조회합니다. (N+1 방지)
     */
    Map<String, BigDecimal> findLatestPricesByTickers(List<String> tickers);
}
