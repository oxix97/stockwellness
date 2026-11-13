package org.stockwellness.application.port.out.stock;

import java.time.LocalDate;
import java.util.List;

public interface IndexDailyReadPort {
    List<IndexPriceObservation> recentFinal(long seriesId, LocalDate asOf, int limit);

    List<IndexPriceObservation> recentFinal(String publicBenchmarkCode, LocalDate asOf, int limit);

    List<IndexPriceObservation> historyFinal(String publicBenchmarkCode, LocalDate start, LocalDate end);
}
