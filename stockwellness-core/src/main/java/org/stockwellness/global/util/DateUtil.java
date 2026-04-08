package org.stockwellness.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 전역 날짜/시간 유틸리티 클래스.
 * 주요 포맷: yyyyMMdd
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateUtil {

    public static final String DATE_PATTERN = "yyyyMMdd";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    /**
     * "yyyyMMdd" 문자열을 LocalDate로 파싱합니다.
     */
    public static LocalDate parse(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || "null".equalsIgnoreCase(dateStr)) {
            return null;
        }
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }

    /**
     * LocalDate를 "yyyyMMdd" 문자열로 포맷팅합니다.
     */
    public static String format(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_FORMATTER);
    }

    /**
     * LocalDate를 java.sql.Date로 변환합니다. (JDBC 연동용)
     */
    public static java.sql.Date toSqlDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return java.sql.Date.valueOf(date);
    }

    /**
     * 현재 날짜(LocalDate)를 반환합니다.
     */
    public static LocalDate today() {
        return LocalDate.now();
    }

    /**
     * 현재 일시(LocalDateTime)를 반환합니다.
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 입력받은 날짜가 null인 경우 오늘 날짜를 반환합니다.
     */
    public static LocalDate getTodayIfNull(LocalDate date) {
        return (date != null) ? date : today();
    }

    /**
     * 두 날짜 사이의 일수 차이를 계산합니다. (d2 - d1)
     */
    public static long daysBetween(LocalDate d1, LocalDate d2) {
        return ChronoUnit.DAYS.between(d1, d2);
    }

    /**
     * 두 일시 사이의 Duration을 계산합니다.
     */
    public static Duration durationBetween(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end);
    }

    /**
     * 대상 날짜가 지정된 기간(시작~종료) 내에 있는지 확인합니다. (inclusive)
     */
    public static boolean isBetween(LocalDate target, LocalDate start, LocalDate end) {
        if (target == null) return false;
        boolean afterStart = (start == null || !target.isBefore(start));
        boolean beforeEnd = (end == null || !target.isAfter(end));
        return afterStart && beforeEnd;
    }

    /**
     * 시작 일시와 종료 일시 사이의 경과 시간(밀리초)을 계산합니다.
     */
    public static long elapsedMillis(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0L;
        return durationBetween(start, end).toMillis();
    }

    /**
     * 특정 단위의 시간을 더합니다.
     */
    public static LocalDateTime plus(LocalDateTime dateTime, long amount, ChronoUnit unit) {
        return dateTime.plus(amount, unit);
    }

    /**
     * 특정 일수를 더합니다.
     */
    public static LocalDate plusDays(LocalDate date, long days) {
        return date.plusDays(days);
    }

    /**
     * 기준일 이전 가장 가까운 직전 평일을 반환합니다.
     * 현재는 주말만 제외하며, 공휴일 캘린더는 반영하지 않습니다.
     */
    public static LocalDate previousBusinessDay(LocalDate date) {
        LocalDate candidate = date.minusDays(1);
        while (candidate.getDayOfWeek() == DayOfWeek.SATURDAY || candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidate = candidate.minusDays(1);
        }
        return candidate;
    }

    /**
     * 현재 시간 기준 만료 여부를 확인합니다.
     */
    public static boolean isExpired(LocalDateTime expiredAt) {
        if (expiredAt == null) return true;
        return now().isAfter(expiredAt);
    }

    /**
     * 한국 주식 시장 개장 여부를 확인합니다. (평일 09:00 ~ 15:30)
     */
    public static boolean isMarketOpen() {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        java.time.DayOfWeek dayOfWeek = now.getDayOfWeek();
        if (dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return false;
        }
        java.time.LocalTime time = now.toLocalTime();
        return !time.isBefore(java.time.LocalTime.of(9, 0)) && !time.isAfter(java.time.LocalTime.of(15, 30));
    }
}
