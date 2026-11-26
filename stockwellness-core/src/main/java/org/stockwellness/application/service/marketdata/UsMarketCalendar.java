package org.stockwellness.application.service.marketdata;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.stockwellness.domain.stock.TradingCalendar;

/**
 * Published 2025-2027 cash-equity sessions for the KIS-supported US index series.
 * Dates outside the verified schedule fail closed until an exchange calendar is reviewed and added.
 */
@Component
public final class UsMarketCalendar {

    private static final int FIRST_VERIFIED_YEAR = 2025;
    private static final int LAST_VERIFIED_YEAR = 2027;
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final LocalTime REGULAR_OPEN = LocalTime.of(9, 30);
    private static final LocalTime REGULAR_CLOSE = LocalTime.of(16, 0);
    private static final LocalTime EARLY_CLOSE = LocalTime.of(13, 0);
    private static final Set<LocalDate> CLOSED_DATES = Set.of(
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 9),
            LocalDate.of(2025, 1, 20),
            LocalDate.of(2025, 2, 17),
            LocalDate.of(2025, 4, 18),
            LocalDate.of(2025, 5, 26),
            LocalDate.of(2025, 6, 19),
            LocalDate.of(2025, 7, 4),
            LocalDate.of(2025, 9, 1),
            LocalDate.of(2025, 11, 27),
            LocalDate.of(2025, 12, 25),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 19),
            LocalDate.of(2026, 2, 16),
            LocalDate.of(2026, 4, 3),
            LocalDate.of(2026, 5, 25),
            LocalDate.of(2026, 6, 19),
            LocalDate.of(2026, 7, 3),
            LocalDate.of(2026, 9, 7),
            LocalDate.of(2026, 11, 26),
            LocalDate.of(2026, 12, 25),
            LocalDate.of(2027, 1, 1),
            LocalDate.of(2027, 1, 18),
            LocalDate.of(2027, 2, 15),
            LocalDate.of(2027, 3, 26),
            LocalDate.of(2027, 5, 31),
            LocalDate.of(2027, 6, 18),
            LocalDate.of(2027, 7, 5),
            LocalDate.of(2027, 9, 6),
            LocalDate.of(2027, 11, 25),
            LocalDate.of(2027, 12, 24));
    private static final Set<LocalDate> EARLY_CLOSE_DATES = Set.of(
            LocalDate.of(2025, 7, 3),
            LocalDate.of(2025, 11, 28),
            LocalDate.of(2025, 12, 24),
            LocalDate.of(2026, 11, 27),
            LocalDate.of(2026, 12, 24),
            LocalDate.of(2027, 11, 26));

    public List<TradingCalendar> calendarDays(String marketCode, LocalDate startDate, LocalDate endDate) {
        validateMarket(marketCode);
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("A valid US market calendar range is required");
        }
        requireVerifiedRange(startDate, endDate);

        List<TradingCalendar> rows = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            rows.add(calendarDay(marketCode, date));
        }
        return List.copyOf(rows);
    }

    public TradingCalendar latestCompletedSession(String marketCode, Instant now) {
        validateMarket(marketCode);
        if (now == null) throw new IllegalArgumentException("Current instant is required");
        LocalDate marketDate = now.atZone(NEW_YORK).toLocalDate();
        requireVerifiedYear(marketDate);
        for (LocalDate date = marketDate; date.getYear() >= FIRST_VERIFIED_YEAR; date = date.minusDays(1)) {
            TradingCalendar day = calendarDay(marketCode, date);
            if (day.isTradingDay() && !now.isBefore(day.getClosesAt().toInstant())) return day;
        }
        throw new IllegalStateException("No completed session in the verified US market calendar for " + marketDate);
    }

    public TradingCalendar latestTradingDayOnOrBefore(String marketCode, LocalDate date) {
        validateMarket(marketCode);
        if (date == null) throw new IllegalArgumentException("A US market date is required");
        requireVerifiedYear(date);
        for (LocalDate candidate = date; candidate.getYear() >= FIRST_VERIFIED_YEAR; candidate = candidate.minusDays(1)) {
            TradingCalendar day = calendarDay(marketCode, candidate);
            if (day.isTradingDay()) return day;
        }
        throw new IllegalStateException("No trading session in the verified US market calendar on or before " + date);
    }

    private static TradingCalendar calendarDay(String marketCode, LocalDate date) {
        requireVerifiedYear(date);
        String source = marketCode + ":OFFICIAL:" + date.getYear();
        boolean tradingDay = date.getDayOfWeek().getValue() <= 5 && !CLOSED_DATES.contains(date);
        if (!tradingDay) return TradingCalendar.of(marketCode, date, false, null, null, source);

        LocalTime close = EARLY_CLOSE_DATES.contains(date) ? EARLY_CLOSE : REGULAR_CLOSE;
        OffsetDateTime opensAt = date.atTime(REGULAR_OPEN).atZone(NEW_YORK).toOffsetDateTime();
        OffsetDateTime closesAt = date.atTime(close).atZone(NEW_YORK).toOffsetDateTime();
        return TradingCalendar.of(marketCode, date, true, opensAt, closesAt, source);
    }

    private static void validateMarket(String marketCode) {
        if (!"NYSE".equals(marketCode) && !"NASDAQ".equals(marketCode)) {
            throw new IllegalArgumentException("Only KIS-supported NYSE and NASDAQ market calendars are supported");
        }
    }

    private static void requireVerifiedYear(LocalDate date) {
        if (date.getYear() < FIRST_VERIFIED_YEAR || date.getYear() > LAST_VERIFIED_YEAR) {
            throw new IllegalArgumentException("No verified calendar is configured for US market year " + date.getYear());
        }
    }

    private static void requireVerifiedRange(LocalDate startDate, LocalDate endDate) {
        requireVerifiedYear(startDate);
        requireVerifiedYear(endDate);
    }
}
