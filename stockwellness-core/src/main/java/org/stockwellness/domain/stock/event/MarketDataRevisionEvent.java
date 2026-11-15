package org.stockwellness.domain.stock.event;

import java.time.LocalDate;

public record MarketDataRevisionEvent(
        String eventId,
        SourceType sourceType,
        long sourceId,
        LocalDate tradeDate,
        String quoteScope,
        int revision
) {
    public MarketDataRevisionEvent {
        if (sourceType == null || sourceId <= 0 || tradeDate == null || revision < 2
                || (sourceType == SourceType.STOCK_PRICE && (quoteScope == null || quoteScope.isBlank()))
                || (sourceType == SourceType.INDEX_PRICE && quoteScope != null)) {
            throw new IllegalArgumentException("Invalid market data revision event");
        }
        String expectedId = idFor(sourceType, sourceId, tradeDate, quoteScope, revision);
        if (eventId == null || eventId.isBlank()) eventId = expectedId;
        if (!eventId.equals(expectedId)) throw new IllegalArgumentException("Revision event id does not match its source");
    }

    public static MarketDataRevisionEvent stock(long stockId, LocalDate tradeDate, String quoteScope, int revision) {
        return new MarketDataRevisionEvent(null, SourceType.STOCK_PRICE, stockId, tradeDate, quoteScope, revision);
    }

    public static MarketDataRevisionEvent index(long seriesId, LocalDate tradeDate, int revision) {
        return new MarketDataRevisionEvent(null, SourceType.INDEX_PRICE, seriesId, tradeDate, null, revision);
    }

    private static String idFor(SourceType sourceType, long sourceId, LocalDate tradeDate, String quoteScope,
            int revision) {
        return sourceType.name() + ":" + sourceId + ":" + (quoteScope == null ? "-" : quoteScope)
                + ":" + tradeDate + ":" + revision;
    }

    public enum SourceType {
        STOCK_PRICE,
        INDEX_PRICE
    }
}
