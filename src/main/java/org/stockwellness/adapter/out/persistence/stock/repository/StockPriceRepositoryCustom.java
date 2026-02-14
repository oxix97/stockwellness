package org.stockwellness.adapter.out.persistence.stock.repository;

import org.stockwellness.application.port.in.stock.result.StockPriceResult;

import java.time.LocalDate;
import java.util.List;

public interface StockPriceRepositoryCustom {
    /**
     * 특정 티커의 연도별 시세 데이터를 조회합니다. (캐싱 용도)
     */
    List<StockPriceResult> findAllByTickerAndYear(String ticker, int year);

    /**
     * 특정 티커의 기간별 시세 데이터를 조회합니다.
     */
    List<StockPriceResult> findAllByTickerAndPeriod(String ticker, LocalDate start, LocalDate end);
}
