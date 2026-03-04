package org.stockwellness.global.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * 금융 데이터 포맷팅을 위한 공통 유틸리티
 */
public class FinanceFormatUtil {

    private static final BigDecimal BILLION = new BigDecimal("100000000"); // 1억

    /**
     * 주가 포맷팅 (천 단위 콤마)
     * 1000원 미만은 소수점 2자리까지 표시
     */
    public static String formatPrice(BigDecimal price) {
        if (price == null) return "0";
        if (price.compareTo(new BigDecimal("1000")) < 0) {
            return new DecimalFormat("#,##0.00").format(price);
        }
        return new DecimalFormat("#,###").format(price);
    }

    /**
     * 등락률 포맷팅 (+/- 부호 포함)
     */
    public static String formatRate(BigDecimal rate) {
        if (rate == null) return "0.00";
        return new DecimalFormat("+#,##0.00;-#").format(rate);
    }

    /**
     * 일반 수치 포맷팅 (소수점 2자리)
     */
    public static String formatDecimal(BigDecimal value) {
        if (value == null) return "0.00";
        return new DecimalFormat("#,##0.00").format(value);
    }

    /**
     * 금액을 '억' 단위로 변환하여 포맷팅
     * @param amount Long 또는 BigDecimal 타입의 금액 (원 단위)
     */
    public static String formatAmount(Object amount) {
        if (amount == null) return "0억";
        
        BigDecimal val;
        if (amount instanceof Long l) val = BigDecimal.valueOf(l);
        else if (amount instanceof BigDecimal b) val = b;
        else return amount.toString();

        if (val.abs().compareTo(BILLION) < 0) {
            return val.toString(); // 1억 미만은 그대로 표시하거나 필요시 로직 추가
        }

        return val.divide(BILLION, 1, RoundingMode.HALF_UP).toString() + "억";
    }
}
