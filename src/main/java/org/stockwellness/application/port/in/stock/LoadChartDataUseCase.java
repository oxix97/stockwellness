package org.stockwellness.application.port.in.stock;

import org.stockwellness.application.port.in.stock.result.ChartDataResponse;
import org.stockwellness.domain.stock.ChartFrequency;
import org.stockwellness.domain.stock.ChartPeriod;

public interface LoadChartDataUseCase {
    ChartDataResponse loadChartData(ChartQuery query);

    record ChartQuery(
            String ticker,
            ChartPeriod period,
            ChartFrequency frequency,
            boolean includeBenchmark
    ) {
    }
}
