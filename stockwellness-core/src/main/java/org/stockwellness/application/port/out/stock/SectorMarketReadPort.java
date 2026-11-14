package org.stockwellness.application.port.out.stock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.stockwellness.domain.stock.insight.LeadingStock;

public interface SectorMarketReadPort {
    Optional<LocalDate> latestFinalDate(String marketCode);

    List<SectorMarketObservation> finalOnDate(LocalDate tradeDate, String marketCode);

    Optional<SectorMarketObservation> latestFinal(String sectorCode, LocalDate asOf);

    List<SectorMarketObservation> historyFinal(long seriesId, LocalDate endDate, int limit);

    List<LeadingStock> leadingStocks(long seriesId, LocalDate tradeDate);
}
