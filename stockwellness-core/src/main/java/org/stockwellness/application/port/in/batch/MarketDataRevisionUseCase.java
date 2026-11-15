package org.stockwellness.application.port.in.batch;

import org.stockwellness.domain.stock.event.MarketDataRevisionEvent;

public interface MarketDataRevisionUseCase {
    void recalculate(MarketDataRevisionEvent event);
}
