package org.stockwellness.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DateUtilTest {

    @Test
    @DisplayName("문자열 'yyyyMMdd'를 LocalDate로 정확히 파싱한다")
    void parse_test() {
        String input = "20260303";
        LocalDate result = DateUtil.parse(input);
        assertThat(result).isEqualTo(LocalDate.of(2026, 3, 3));
    }

    @Test
    @DisplayName("잘못된 입력(null, empty, 'null')은 null을 반환한다")
    void parse_edge_cases() {
        assertThat(DateUtil.parse(null)).isNull();
        assertThat(DateUtil.parse(" ")).isNull();
        assertThat(DateUtil.parse("null")).isNull();
    }

    @Test
    @DisplayName("LocalDate를 'yyyyMMdd' 문자열로 정확히 포맷팅한다")
    void format_test() {
        LocalDate input = LocalDate.of(2026, 3, 3);
        String result = DateUtil.format(input);
        assertThat(result).isEqualTo("20260303");
    }

    @Test
    @DisplayName("LocalDate를 java.sql.Date로 정확히 변환한다")
    void toSqlDate_test() {
        LocalDate input = LocalDate.of(2026, 3, 3);
        java.sql.Date result = DateUtil.toSqlDate(input);
        assertThat(result.toString()).isEqualTo("2026-03-03");
    }

    @Test
    @DisplayName("두 날짜 사이의 일수 차이를 정확히 계산한다")
    void daysBetween_test() {
        LocalDate d1 = LocalDate.of(2026, 3, 1);
        LocalDate d2 = LocalDate.of(2026, 3, 3);
        assertThat(DateUtil.daysBetween(d1, d2)).isEqualTo(2L);
    }

    @Test
    @DisplayName("지정된 기간 내에 날짜가 포함되는지 정확히 확인한다")
    void isBetween_test() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 3);
        
        assertThat(DateUtil.isBetween(LocalDate.of(2026, 3, 2), start, end)).isTrue();
        assertThat(DateUtil.isBetween(start, start, end)).isTrue();
        assertThat(DateUtil.isBetween(end, start, end)).isTrue();
        assertThat(DateUtil.isBetween(LocalDate.of(2026, 3, 4), start, end)).isFalse();
    }

    @Test
    @DisplayName("경과 시간(밀리초)을 정확히 계산한다")
    void elapsedMillis_test() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 3, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 3, 10, 0, 1); // 1초 차이
        
        assertThat(DateUtil.elapsedMillis(start, end)).isEqualTo(1000L);
    }

    @Test
    @DisplayName("입력이 null이면 오늘 날짜를, 아니면 입력 날짜를 반환한다")
    void getTodayIfNull_test() {
        LocalDate input = LocalDate.of(2020, 1, 1);
        assertThat(DateUtil.getTodayIfNull(input)).isEqualTo(input);
        assertThat(DateUtil.getTodayIfNull(null)).isEqualTo(DateUtil.today());
    }

    @Test
    @DisplayName("두 일시 사이의 Duration을 정확히 계산한다")
    void durationBetween_test() {
        LocalDateTime start = LocalDateTime.of(2026, 3, 3, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 3, 10, 0, 5);
        assertThat(DateUtil.durationBetween(start, end)).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("특정 단위의 시간을 정확히 더한다")
    void plus_test() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 3, 10, 0, 0);
        assertThat(DateUtil.plus(now, 1, ChronoUnit.HOURS))
                .isEqualTo(LocalDateTime.of(2026, 3, 3, 11, 0, 0));
    }

    @Test
    @DisplayName("특정 일수를 정확히 더한다")
    void plusDays_test() {
        LocalDate today = LocalDate.of(2026, 3, 3);
        assertThat(DateUtil.plusDays(today, 1)).isEqualTo(LocalDate.of(2026, 3, 4));
    }

    @Test
    @DisplayName("현재 시간 기준 만료 여부를 정확히 판단한다")
    void isExpired_test() {
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        
        assertThat(DateUtil.isExpired(future)).isFalse();
        assertThat(DateUtil.isExpired(past)).isTrue();
        assertThat(DateUtil.isExpired(null)).isTrue();
    }
}
