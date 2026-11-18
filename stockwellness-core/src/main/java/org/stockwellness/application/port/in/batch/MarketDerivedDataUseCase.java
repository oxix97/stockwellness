package org.stockwellness.application.port.in.batch;

import java.time.LocalDate;

public interface MarketDerivedDataUseCase {
    void calculateStockIndicators(long stockId, String quoteScope, LocalDate targetDate);

    void calculateIndexIndicators(long seriesId, LocalDate targetDate);

    void calculateIndexWeather(long seriesId, LocalDate targetDate);

    void calculateSectorLeadingStocks(long seriesId, LocalDate targetDate);
}
