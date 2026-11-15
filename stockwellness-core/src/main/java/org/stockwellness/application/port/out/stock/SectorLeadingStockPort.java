package org.stockwellness.application.port.out.stock;

import java.time.LocalDate;
import java.util.List;

public interface SectorLeadingStockPort {
    List<SectorLeadingStockCandidate> finalCandidates(long seriesId, LocalDate tradeDate);

    void replace(long seriesId, LocalDate tradeDate, List<RankedSectorStock> stocks, String inputManifest);

    record RankedSectorStock(long stockId, LocalDate priceDate, String quoteScope, int rank) {}
}
