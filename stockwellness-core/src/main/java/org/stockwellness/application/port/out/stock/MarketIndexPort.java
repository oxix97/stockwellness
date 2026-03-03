package org.stockwellness.application.port.out.stock;

import org.stockwellness.domain.stock.insight.MarketIndex;
import java.util.List;

public interface MarketIndexPort {
    List<MarketIndex> findAll();
}
