package org.stockwellness.global.util;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * ta4j와 도메인 모델(BigDecimal) 간의 변환을 담당하는 유틸리티
 */
public class QuantMapper {

    private static final int DEFAULT_SCALE = 4;

    /**
     * OHLC 데이터와 실제 날짜 리스트를 ta4j BarSeries로 변환합니다.
     */
    public static BarSeries toBarSeries(String name, List<BigDecimal> highs, List<BigDecimal> lows, List<BigDecimal> closes, List<LocalDate> dates) {
        BarSeries series = new BaseBarSeriesBuilder().withName(name).build();
        ZoneId zone = ZoneId.systemDefault();

        for (int i = 0; i < closes.size(); i++) {
            if (closes.get(i) == null || highs.get(i) == null || lows.get(i) == null) continue;
            // 실제 날짜를 ZonedDateTime으로 변환하여 추가 (2월 30일 등 유효하지 않은 날짜 오류 방지)
            ZonedDateTime zdt = dates.get(i).atStartOfDay(zone);
            series.addBar(
                    zdt,
                    closes.get(i), highs.get(i), lows.get(i), closes.get(i), BigDecimal.ONE
            );
        }
        return series;
    }

    /**
     * 가상의 날짜를 생성하여 BarSeries를 만듭니다. (날짜가 중요하지 않은 테스트나 단순 계산용)
     * 유효하지 않은 날짜(2월 30일 등)가 생성되지 않도록 안전하게 plusDays() 방식으로 생성합니다.
     */
    public static BarSeries toBarSeries(String name, List<BigDecimal> highs, List<BigDecimal> lows, List<BigDecimal> closes) {
        BarSeries series = new BaseBarSeriesBuilder().withName(name).build();
        // 기준일(오늘)에서 역산하여 시작일을 잡고, 하루씩 더해가며 유효한 날짜 보장
        ZonedDateTime cursor = ZonedDateTime.now().minusDays(closes.size() + 1);

        for (int i = 0; i < closes.size(); i++) {
            cursor = cursor.plusDays(1);
            if (closes.get(i) == null || highs.get(i) == null || lows.get(i) == null) continue;
            series.addBar(
                    cursor,
                    closes.get(i), highs.get(i), lows.get(i), closes.get(i), BigDecimal.ONE
            );
        }
        return series;
    }

    /**
     * ta4j의 Num 타입을 BigDecimal로 변환하며 Scale을 조정합니다.
     */
    public static BigDecimal toBigDecimal(Num num) {
        return toBigDecimal(num, DEFAULT_SCALE);
    }

    public static BigDecimal toBigDecimal(Num num, int scale) {
        if (num == null || num.isNaN()) return null;
        return new BigDecimal(num.toString()).setScale(scale, RoundingMode.HALF_UP);
    }
}
