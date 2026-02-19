package org.stockwellness.application.port.out.stock;

import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.domain.stock.price.StockPrice;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LoadStockPricePort {
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
}
