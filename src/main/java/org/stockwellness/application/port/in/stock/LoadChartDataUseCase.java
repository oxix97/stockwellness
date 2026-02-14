package org.stockwellness.application.port.in.stock;

import org.stockwellness.application.port.in.stock.result.ChartDataResponse;

import java.time.LocalDate;

public interface LoadChartDataUseCase {
    ChartDataResponse loadChartData(ChartQuery query);

    record ChartQuery(
            String ticker,
            String period,    // 1W, 1M, 3M, 1Y, 3Y, 5Y, ALL
            String frequency, // DAILY, WEEKLY, MONTHLY
            boolean includeBenchmark
    ) {
    }
}
