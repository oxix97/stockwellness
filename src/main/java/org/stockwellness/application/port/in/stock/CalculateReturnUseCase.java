package org.stockwellness.application.port.in.stock;

import org.stockwellness.application.port.in.stock.result.ReturnRateResponse;

public interface CalculateReturnUseCase {
    ReturnRateResponse calculateReturn(String ticker, String period);
}
