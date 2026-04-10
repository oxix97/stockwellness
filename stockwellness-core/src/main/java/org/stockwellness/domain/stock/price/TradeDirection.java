package org.stockwellness.domain.stock.price;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeDirection {
    BUY("매수"),
    SELL("매도");

    private final String description;
}
