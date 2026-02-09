package org.stockwellness.domain.stock;

import lombok.Getter;

@Getter
public enum StockStatus {
    ACTIVE("정상상태"),
    DELISTED("상장폐지"),
    SUSPENDED("거래정지"),
    ADMINISTRATIVE("관리종목");

    private final String description;

    StockStatus(String description) {
        this.description = description;
    }
}
