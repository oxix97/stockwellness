package org.stockwellness.domain.stock.price;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.domain.stock.Stock;

/** One source observation of raw EOD prices for a market scope and trade date. */
@Entity
@Table(name = "stock_price_daily")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EodStockPrice {
    @EmbeddedId
    private EodStockPriceId id;

    @MapsId("stockId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "open_price", precision = 19, scale = 4)
    private BigDecimal openPrice;
    @Column(name = "high_price", precision = 19, scale = 4)
    private BigDecimal highPrice;
    @Column(name = "low_price", precision = 19, scale = 4)
    private BigDecimal lowPrice;
    @Column(name = "close_price", precision = 19, scale = 4)
    private BigDecimal closePrice;
    @Column(name = "volume")
    private Long volume;
    @Column(name = "trading_amount", precision = 25, scale = 2)
    private BigDecimal tradingAmount;
    @Column(name = "source", nullable = false, length = 100)
    private String source;
    @Column(name = "data_status", nullable = false, length = 20)
    private String dataStatus;
    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;
    @Column(name = "corrected_at")
    private Instant correctedAt;
    @Column(name = "revision", nullable = false)
    private Integer revision;

    public static EodStockPrice of(Stock stock, java.time.LocalDate tradeDate, String quoteScope,
            BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, Long volume,
            BigDecimal tradingAmount, String source, String dataStatus) {
        if (stock == null || tradeDate == null || quoteScope == null || quoteScope.isBlank()
                || quoteScope.length() > 20 || source == null || source.isBlank() || source.length() > 100
                || dataStatus == null || !Set.of("PROVISIONAL", "FINAL", "INCOMPLETE").contains(dataStatus)
                || below(high, low) || below(high, open) || below(high, close)
                || above(low, open) || above(low, close)
                || ("FINAL".equals(dataStatus) && (open == null || high == null || low == null || close == null))
                || (volume != null && volume < 0) || negative(open) || negative(high)
                || negative(low) || negative(close) || negative(tradingAmount)) {
            throw new IllegalArgumentException("Invalid EOD quote");
        }
        EodStockPrice value = new EodStockPrice();
        value.id = new EodStockPriceId(stock.getId(), tradeDate, quoteScope);
        value.stock = stock;
        value.openPrice = open;
        value.highPrice = high;
        value.lowPrice = low;
        value.closePrice = close;
        value.volume = volume;
        value.tradingAmount = tradingAmount;
        value.source = source;
        value.dataStatus = dataStatus;
        value.collectedAt = Instant.now();
        value.revision = 1;
        return value;
    }

    public boolean reviseFrom(EodStockPrice source, Instant now) {
        if (statusRank(source.dataStatus) < statusRank(dataStatus)) return false;
        boolean changed = !same(openPrice, source.openPrice) || !same(highPrice, source.highPrice)
                || !same(lowPrice, source.lowPrice) || !same(closePrice, source.closePrice)
                || !Objects.equals(volume, source.volume) || !same(tradingAmount, source.tradingAmount)
                || !Objects.equals(dataStatus, source.dataStatus) || !Objects.equals(this.source, source.source);
        collectedAt = now;
        if (changed) {
            openPrice = source.openPrice;
            highPrice = source.highPrice;
            lowPrice = source.lowPrice;
            closePrice = source.closePrice;
            volume = source.volume;
            tradingAmount = source.tradingAmount;
            dataStatus = source.dataStatus;
            this.source = source.source;
            revision++;
            correctedAt = now;
        }
        return changed;
    }

    private static int statusRank(String status) {
        return switch (status) {
            case "INCOMPLETE" -> 0;
            case "PROVISIONAL" -> 1;
            case "FINAL" -> 2;
            default -> throw new IllegalArgumentException("Unknown EOD data status: " + status);
        };
    }

    private static boolean same(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private static boolean below(BigDecimal value, BigDecimal lower) {
        return value != null && lower != null && value.compareTo(lower) < 0;
    }

    private static boolean above(BigDecimal value, BigDecimal upper) {
        return value != null && upper != null && value.compareTo(upper) > 0;
    }

    private static boolean negative(BigDecimal value) { return value != null && value.signum() < 0; }
}
