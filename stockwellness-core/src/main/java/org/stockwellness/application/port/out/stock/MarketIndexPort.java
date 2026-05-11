package org.stockwellness.application.port.out.stock;

import java.util.List;

import org.stockwellness.domain.stock.insight.MarketIndex;

public interface MarketIndexPort {
    List<MarketIndex> findAll();
}
