package org.stockwellness.application.service.batch;

import java.util.OptionalLong;
import org.stockwellness.domain.stock.MarketType;

/** Validates source units without persisting a value that has no source as-of date. */
public final class ListedSharesNormalizer {
    private ListedSharesNormalizer() {}

    public static OptionalLong normalize(MarketType market, String sourceValue) {
        if (sourceValue == null || sourceValue.isBlank()) return OptionalLong.empty();
        if (market == null) throw new IllegalArgumentException("Market is required for listed-share normalization");
        try {
            long value = Long.parseLong(sourceValue.trim());
            if (value < 0) throw new IllegalArgumentException("Listed shares cannot be negative");
            if (market == MarketType.KOSDAQ) value = Math.multiplyExact(value, 1_000L);
            return OptionalLong.of(value);
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException("Invalid listed-share value or unit overflow", exception);
        }
    }
}
