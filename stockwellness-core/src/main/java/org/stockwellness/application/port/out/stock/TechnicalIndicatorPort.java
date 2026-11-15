package org.stockwellness.application.port.out.stock;

import org.stockwellness.domain.stock.insight.IndexTechnicalIndicator;
import org.stockwellness.domain.stock.price.StockTechnicalIndicator;

public interface TechnicalIndicatorPort {
    void upsertStock(StockTechnicalIndicator value);

    void upsertIndex(IndexTechnicalIndicator value);
}
