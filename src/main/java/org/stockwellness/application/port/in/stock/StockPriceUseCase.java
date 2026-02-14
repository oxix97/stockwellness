package org.stockwellness.application.port.in.stock;

import org.stockwellness.application.port.in.stock.result.ChartDataResponse;
import org.stockwellness.application.port.in.stock.result.ReturnRateResponse;
import org.stockwellness.domain.stock.ChartFrequency;
import org.stockwellness.domain.stock.ChartPeriod;

/**
 * 주식 가격 조회 및 수익률 계산을 담당하는 통합 유스케이스
 */
public interface StockPriceUseCase {

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
