package org.stockwellness.application.port.in.stock;

import org.stockwellness.application.port.in.stock.result.ChartDataResponse;
import org.stockwellness.application.port.in.stock.result.ReturnRateResponse;
import org.stockwellness.application.port.in.stock.result.StockSupplyRankingResponse;
import org.stockwellness.domain.stock.price.ChartFrequency;
import org.stockwellness.domain.stock.price.ChartPeriod;
import org.stockwellness.domain.stock.price.TradeDirection;

/**
 * 주식 가격 조회 및 수익률 계산을 담당하는 통합 유스케이스
 */
public interface StockPriceUseCase {

    /**
     * 수급(외인/기관) 상위 종목 목록을 조회합니다. (전체 데이터 중 가장 최신 날짜 기준)
     */
    StockSupplyRankingResponse getTopStocksBySupply(
            TradeDirection direction,
            int limit
    );

    /**
     * 차트용 과거 가격 데이터를 조회합니다.
     */
    ChartDataResponse loadChartData(ChartQuery query);

    /**
     * 특정 기간의 수익률을 계산합니다.
     */
    ReturnRateResponse calculateReturn(String ticker, ChartPeriod period);

    record ChartQuery(
            String ticker,
            ChartPeriod period,
            ChartFrequency frequency,
            boolean includeBenchmark
    ) {
    }
}
