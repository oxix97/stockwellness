package org.stockwellness.application.port.out.stock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.stockwellness.domain.stock.TradingCalendar;

/** Reads provider-backed market-day facts; an empty result means UNKNOWN, never CLOSED or OPEN. */
public interface TradingCalendarPort {
    Optional<TradingCalendar> find(String marketCode, LocalDate tradeDate);
    List<TradingCalendar> findAll(String marketCode, LocalDate startDate, LocalDate endDate);
    Optional<LocalDate> latestTradingDayOnOrBefore(String marketCode, LocalDate tradeDate);
    void upsert(List<TradingCalendar> rows);
}
