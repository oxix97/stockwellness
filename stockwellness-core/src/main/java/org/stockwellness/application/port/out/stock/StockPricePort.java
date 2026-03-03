package org.stockwellness.application.port.out.stock;

import org.stockwellness.adapter.out.external.kis.dto.KisMultiStockPriceDetail;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface StockPricePort {
    /**
     * 특정 종목들의 지정된 날짜 시세 데이터를 조회합니다.
     */
    List<StockPrice> findByStocksAndDate(List<Stock> stocks, LocalDate date);

    /**
     * 지정된 날짜의 모든 종목 시세 데이터를 조회합니다.
     */
    List<StockPrice> findAllByDate(LocalDate date);

    Optional<StockPrice> findLateststockPrice(String isinCode);

    List<StockPrice> loadRecentHistories(String isinCode, int limit);

    Map<String, List<StockPrice>> loadRecentHistoriesBatch(List<String> isinCodes, int limit);

    /**
     * 티커와 연도를 기준으로 일봉 데이터를 로드합니다. (캐싱 단위)
     */
    List<StockPriceResult> loadPricesByYear(String ticker, int year);

    /**
     * 티커와 기간을 기준으로 일봉 데이터를 로드합니다.
     */
    List<StockPriceResult> loadPricesByTicker(String ticker, LocalDate start, LocalDate end);

    /**
     * 특정 종목의 최근 종가 리스트를 조회합니다. (지표 계산용)
     */
    List<BigDecimal> findRecentClosingPrices(Stock stock, LocalDate date, int limit);

    /**
     * 특정 종목의 가장 최근 저장된 날짜를 조회합니다.
     */
    LocalDate findLatestBaseDate(Stock stock);

    /**
     * 여러 종목의 마지막 저장일들을 한 번에 조회합니다.
     */
    Map<Long, LocalDate> findLatestBaseDatesByStocks(List<Stock> stocks);

    /**
     * 여러 종목의 과거 종가 리스트를 한 번에 조회합니다.
     */
    Map<Long, List<BigDecimal>> findRecentClosingPricesByStocks(List<Stock> stocks, LocalDate date, int limit);

    /**
     * 외부 API를 통해 멀티 종목 시세를 조회합니다. (당일용)
     */
    List<KisMultiStockPriceDetail> fetchMultiStockPrices(List<String> tickers);

    // FetchStockPricePort에서 통합된 메서드
    List<Stock> fetchDaily(LocalDate date);
    List<StockPrice> fetchDailyPrice(LocalDate date);
}
