package org.stockwellness.adapter.out.persistence.stock.repository;

import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.AlignmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface StockPriceRepositoryCustom {
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
}
