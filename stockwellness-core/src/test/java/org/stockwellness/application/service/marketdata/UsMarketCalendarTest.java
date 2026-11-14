package org.stockwellness.application.service.marketdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.stock.TradingCalendar;

class UsMarketCalendarTest {

    private final UsMarketCalendar calendar = new UsMarketCalendar();

    @Test
    void published_2025_calendar_includes_regular_holidays_special_closure_and_early_closes() {
        var rows = calendar.calendarDays("NASDAQ", LocalDate.of(2025, 1, 9), LocalDate.of(2025, 1, 9));
        TradingCalendar carterMourning = rows.getFirst();
        TradingCalendar blackFriday = calendar.calendarDays("NYSE", LocalDate.of(2025, 11, 28),
                LocalDate.of(2025, 11, 28)).getFirst();
        var fullYear = calendar.calendarDays("NASDAQ", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

        assertThat(carterMourning.isTradingDay()).isFalse();
        assertThat(carterMourning.getSource()).isEqualTo("NASDAQ:OFFICIAL:2025");
        assertThat(blackFriday.getClosesAt()).isEqualTo(OffsetDateTime.parse("2025-11-28T13:00-05:00"));
        assertThat(fullYear.stream().filter(TradingCalendar::isTradingDay)).hasSize(250);
        assertThat(fullYear.stream().filter(day -> day.isTradingDay()
                && day.getClosesAt().toLocalTime().equals(java.time.LocalTime.of(13, 0)))).hasSize(3);
    }

    @Test
    void published_2026_calendar_marks_thanksgiving_closed_and_day_after_early_close() {
        var rows = calendar.calendarDays("NASDAQ", LocalDate.of(2026, 11, 26), LocalDate.of(2026, 11, 27));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).isTradingDay()).isFalse();
        assertThat(rows.get(1).isTradingDay()).isTrue();
        assertThat(rows.get(1).getClosesAt())
                .isEqualTo(OffsetDateTime.parse("2026-11-27T13:00-05:00"));
    }

    @Test
    void published_2026_calendar_marks_christmas_eve_as_early_close() {
        TradingCalendar session = calendar.calendarDays("NYSE", LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 24))
                .getFirst();

        assertThat(session.isTradingDay()).isTrue();
        assertThat(session.getOpensAt()).isEqualTo(OffsetDateTime.parse("2026-12-24T09:30-05:00"));
        assertThat(session.getClosesAt()).isEqualTo(OffsetDateTime.parse("2026-12-24T13:00-05:00"));
    }

    @Test
    void july_2_2026_remains_a_regular_session_before_observed_independence_day() {
        TradingCalendar session = calendar.calendarDays("NASDAQ", LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 2))
                .getFirst();

        assertThat(session.isTradingDay()).isTrue();
        assertThat(session.getClosesAt()).isEqualTo(OffsetDateTime.parse("2026-07-02T16:00-04:00"));
    }

    @Test
    void full_2026_schedule_has_only_exchange_published_weekday_sessions() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);

        var rows = calendar.calendarDays("NASDAQ", start, end);

        assertThat(rows).hasSize(365);
        assertThat(rows.stream().filter(TradingCalendar::isTradingDay)).hasSize(251);
        assertThat(rows.stream().filter(day -> day.isTradingDay()
                && day.getClosesAt().toLocalTime().equals(java.time.LocalTime.of(13, 0)))).hasSize(2);
    }

    @Test
    void session_offsets_follow_new_york_dst() {
        TradingCalendar summer = calendar.calendarDays("NASDAQ", LocalDate.of(2026, 7, 13), LocalDate.of(2026, 7, 13))
                .getFirst();
        TradingCalendar winter = calendar.calendarDays("NYSE", LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 13))
                .getFirst();

        assertThat(summer.getClosesAt().toInstant()).isEqualTo(Instant.parse("2026-07-13T20:00:00Z"));
        assertThat(winter.getClosesAt().toInstant()).isEqualTo(Instant.parse("2026-11-13T21:00:00Z"));
        assertThat(summer.getClosesAt().getOffset()).isEqualTo(ZoneId.of("America/New_York").getRules()
                .getOffset(Instant.parse("2026-07-13T20:00:00Z")));
    }

    @Test
    void latest_completed_session_respects_early_close_and_holiday() {
        assertThat(calendar.latestCompletedSession("NYSE", Instant.parse("2026-11-27T17:59:59Z")).getTradeDate())
                .isEqualTo(LocalDate.of(2026, 11, 25));
        assertThat(calendar.latestCompletedSession("NYSE", Instant.parse("2026-11-27T18:00:00Z")).getTradeDate())
                .isEqualTo(LocalDate.of(2026, 11, 27));
        assertThat(calendar.latestCompletedSession("NYSE", Instant.parse("2026-11-26T22:00:00Z")).getTradeDate())
                .isEqualTo(LocalDate.of(2026, 11, 25));
    }

    @Test
    void latest_trading_session_on_or_before_requested_date_skips_holidays_and_weekends() {
        assertThat(calendar.latestTradingDayOnOrBefore("NYSE", LocalDate.of(2026, 11, 28)).getTradeDate())
                .isEqualTo(LocalDate.of(2026, 11, 27));
        assertThat(calendar.latestTradingDayOnOrBefore("NASDAQ", LocalDate.of(2026, 11, 26)).getTradeDate())
                .isEqualTo(LocalDate.of(2026, 11, 25));
    }

    @Test
    void unsupported_markets_and_years_fail_closed() {
        assertThatThrownBy(() -> calendar.calendarDays("COMP", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calendar.calendarDays("NYSE", LocalDate.of(2027, 1, 4), LocalDate.of(2027, 1, 4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("verified calendar");
    }
}
