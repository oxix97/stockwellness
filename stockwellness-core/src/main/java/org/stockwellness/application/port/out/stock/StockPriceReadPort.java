package org.stockwellness.application.port.out.stock;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.stockwellness.domain.stock.price.PriceBasis;

public interface StockPriceReadPort {
    Map<Long, List<PriceObservation>> history(Set<Long> stockIds, String quoteScope,
            LocalDate start, LocalDate end, PriceBasis basis);

    Map<Long, List<PriceObservation>> recent(Set<Long> stockIds, String quoteScope,
            LocalDate asOf, int limit, PriceBasis basis);

    Map<Long, PriceObservation> latest(Set<Long> stockIds, String quoteScope,
            LocalDate asOf, PriceBasis basis);
}
