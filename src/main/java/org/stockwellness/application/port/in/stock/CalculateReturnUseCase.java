package org.stockwellness.application.port.in.stock;

import org.stockwellness.application.port.in.stock.result.ReturnRateResponse;
import org.stockwellness.domain.stock.ChartPeriod;

public interface CalculateReturnUseCase {
    ReturnRateResponse calculateReturn(String ticker, ChartPeriod period);
}
