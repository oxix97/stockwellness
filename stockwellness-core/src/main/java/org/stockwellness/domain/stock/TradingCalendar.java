package org.stockwellness.domain.stock;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.global.error.exception.GlobalException;

import static org.stockwellness.global.error.ErrorCode.INVALID_INPUT_VALUE;

@Entity
@Table(name = "trading_calendar")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradingCalendar {
    @EmbeddedId
    private TradingCalendarId id;

    @Column(name = "is_trading_day", nullable = false)
    private boolean tradingDay;

    @Column(name = "opens_at")
    private OffsetDateTime opensAt;

    @Column(name = "closes_at")
    private OffsetDateTime closesAt;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "collected_at", nullable = false)
    private OffsetDateTime collectedAt;

    public static TradingCalendar of(String marketCode, LocalDate tradeDate, boolean tradingDay,
            OffsetDateTime opensAt, OffsetDateTime closesAt, String source) {
        if (marketCode == null || !marketCode.matches("[A-Z0-9_-]{1,20}") || tradeDate == null
                || source == null || source.isBlank() || source.length() > 50
                || (opensAt == null) != (closesAt == null)
                || (!tradingDay && opensAt != null)
                || (opensAt != null && !opensAt.isBefore(closesAt))) throw invalidInput();
        TradingCalendar result = new TradingCalendar();
        result.id = new TradingCalendarId(marketCode, tradeDate);
        result.tradingDay = tradingDay;
        result.opensAt = opensAt;
        result.closesAt = closesAt;
        result.source = source;
        result.collectedAt = OffsetDateTime.now(ZoneOffset.UTC);
        return result;
    }

    public String getMarketCode() { return id.getMarketCode(); }
    public LocalDate getTradeDate() { return id.getTradeDate(); }
    public boolean isTradingDay() { return tradingDay; }

    private static GlobalException invalidInput() {
        return new GlobalException(INVALID_INPUT_VALUE);
    }
}
