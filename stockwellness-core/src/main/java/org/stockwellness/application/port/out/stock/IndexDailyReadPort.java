package org.stockwellness.application.port.out.stock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

public interface IndexDailyReadPort {
    List<IndexPriceObservation> recentFinal(long seriesId, LocalDate asOf, int limit);

    List<IndexPriceObservation> recentFinal(String publicBenchmarkCode, LocalDate asOf, int limit);

    List<IndexPriceObservation> historyFinal(String publicBenchmarkCode, LocalDate start, LocalDate end);

    Optional<IndexPriceObservation> latestFinal(String publicBenchmarkCode, LocalDate asOf);

    List<LocalDate> finalDatesFrom(long seriesId, LocalDate startDate, int limit);

    Optional<BigDecimal> finalWeatherScore(String publicBenchmarkCode, LocalDate tradeDate);
}
