package org.stockwellness.application.port.out.stock;

import java.util.List;
import org.stockwellness.domain.stock.StockRiskStatus;

public interface StockRiskStatusPort {
    void upsert(List<StockRiskStatus> values);
}
