package org.stockwellness.domain.stock;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradingCalendarId implements Serializable {
    @Column(name = "market_code", nullable = false, length = 20)
    private String marketCode;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    public TradingCalendarId(String marketCode, LocalDate tradeDate) {
        this.marketCode = marketCode;
        this.tradeDate = tradeDate;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TradingCalendarId that)) return false;
        return Objects.equals(marketCode, that.marketCode) && Objects.equals(tradeDate, that.tradeDate);
    }

    @Override public int hashCode() {
        return Objects.hash(marketCode, tradeDate);
    }
}
