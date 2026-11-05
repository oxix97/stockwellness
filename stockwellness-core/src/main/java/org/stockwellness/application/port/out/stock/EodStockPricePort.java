package org.stockwellness.application.port.out.stock;

import java.util.List;
import org.stockwellness.domain.stock.price.EodStockPrice;

public interface EodStockPricePort {
    void upsert(List<EodStockPrice> values);
}
