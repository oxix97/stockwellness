package org.stockwellness.application.port.out.stock;

import java.util.List;
import org.stockwellness.domain.stock.price.EodStockPrice;
import org.stockwellness.domain.stock.price.StockPriceAdjustment;

public interface EodStockPricePort {
    void upsert(List<EodStockPrice> values);
    void upsertAdjustments(List<StockPriceAdjustment> values);
    void upsertObservations(List<EodStockPrice> prices, List<StockPriceAdjustment> adjustments);
}
