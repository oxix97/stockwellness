package org.stockwellness.global.common.util;

import java.math.BigDecimal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParsingUtil {

    public static BigDecimal toBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            // Trim and remove commas before parsing
            String cleaned = value.trim().replace(",", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.warn("[ParsingUtil] BigDecimal 변환 실패: value={}, error={}", value, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    public static Long toLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            // Trim and remove commas before parsing
            String cleaned = value.trim().replace(",", "");
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            log.warn("[ParsingUtil] Long 변환 실패: value={}, error={}", value, e.getMessage());
            return 0L;
        }
    }
}
