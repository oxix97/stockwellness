package org.stockwellness.domain.stock;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.stockwellness.global.error.exception.GlobalException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TradingCalendarTest {
    @Test
    void distinguishes_open_day_closed_day_and_unrecorded_date() {
        var open = TradingCalendar.of("KOSPI", LocalDate.parse("2026-10-05"), true,
                OffsetDateTime.of(2026, 10, 5, 9, 0, 0, 0, ZoneOffset.ofHours(9)),
                OffsetDateTime.of(2026, 10, 5, 15, 30, 0, 0, ZoneOffset.ofHours(9)), "CTCA0903R");
        var closed = TradingCalendar.of("KOSPI", LocalDate.parse("2026-10-03"), false, null, null, "CTCA0903R");

        assertThat(open.isTradingDay()).isTrue();
        assertThat(open.getMarketCode()).isEqualTo("KOSPI");
        assertThat(closed.isTradingDay()).isFalse();
        assertThat(closed.getOpensAt()).isNull();
    }

    @Test
    void rejects_reversed_or_partial_sessions_and_a_session_on_a_closed_day() {
        var date = LocalDate.parse("2026-10-05");
        var open = OffsetDateTime.of(2026, 10, 5, 9, 0, 0, 0, ZoneOffset.ofHours(9));
        var close = OffsetDateTime.of(2026, 10, 5, 15, 30, 0, 0, ZoneOffset.ofHours(9));
        assertThatThrownBy(() -> TradingCalendar.of("KOSPI", date, true, close, open, "CTCA0903R"))
                .isInstanceOf(GlobalException.class);
        assertThatThrownBy(() -> TradingCalendar.of("KOSPI", date, true, open, null, "CTCA0903R"))
                .isInstanceOf(GlobalException.class);
        assertThatThrownBy(() -> TradingCalendar.of("KOSPI", date, false, open, close, "CTCA0903R"))
                .isInstanceOf(GlobalException.class);
    }
}
