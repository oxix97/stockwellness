package org.stockwellness.domain.stock.price;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.domain.stock.Stock;

/** Provider-adjusted price observation kept separate from raw EOD prices. */
@Entity
@Table(name = "stock_price_adjustment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockPriceAdjustment {
    @EmbeddedId
    private EodStockPriceId id;

    @Column(name = "adjustment_basis", nullable = false, length = 30)
    private String adjustmentBasis;
    @Column(name = "adjustment_as_of", nullable = false)
    private LocalDate adjustmentAsOf;
    @Column(name = "open_price", precision = 19, scale = 4)
    private BigDecimal openPrice;
    @Column(name = "high_price", precision = 19, scale = 4)
    private BigDecimal highPrice;
    @Column(name = "low_price", precision = 19, scale = 4)
    private BigDecimal lowPrice;
    @Column(name = "adjusted_close", nullable = false, precision = 19, scale = 4)
    private BigDecimal adjustedClose;
    @Column(name = "source", nullable = false, length = 100)
    private String source;
    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;

    public static StockPriceAdjustment of(Stock stock, LocalDate tradeDate, String quoteScope,
            LocalDate adjustmentAsOf, BigDecimal open, BigDecimal high, BigDecimal low,
            BigDecimal adjustedClose, String source) {
        if (stock == null || tradeDate == null || quoteScope == null || quoteScope.isBlank()
                || quoteScope.length() > 20 || adjustmentAsOf == null || adjustmentAsOf.isBefore(tradeDate)
                || negative(open) || negative(high) || negative(low) || negative(adjustedClose)
                || adjustedClose == null || (high != null && low != null && high.compareTo(low) < 0)
                || source == null || source.isBlank() || source.length() > 100) {
            throw new IllegalArgumentException("Invalid provider-adjusted price observation");
        }
        StockPriceAdjustment value = new StockPriceAdjustment();
        value.id = new EodStockPriceId(stock.getId(), tradeDate, quoteScope);
        value.adjustmentBasis = "PROVIDER_ADJUSTED";
        value.adjustmentAsOf = adjustmentAsOf;
        value.openPrice = open;
        value.highPrice = high;
        value.lowPrice = low;
        value.adjustedClose = adjustedClose;
        value.source = source;
        value.collectedAt = Instant.now();
        return value;
    }

    private static boolean negative(BigDecimal value) { return value != null && value.signum() < 0; }
}
