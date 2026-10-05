package org.stockwellness.domain.stock.price;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

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

/** Source-verified quote model. Legacy StockPrice remains the active compatibility model. */
@Entity
@Table(name = "stock_price_eod")
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
    @Column(name = "previous_close_price", precision = 19, scale = 4)
    private BigDecimal previousClosePrice;
    @Column(name = "volume")
    private Long volume;
    @Column(name = "trading_amount", precision = 25, scale = 2)
    private BigDecimal tradingAmount;
    @Column(name = "no_trade")
    private Boolean noTrade;
    @Column(name = "source_api", nullable = false, length = 50)
    private String sourceApi;
    @Column(name = "data_status", nullable = false, length = 20)
    private String dataStatus;
    @Column(name = "collected_at", nullable = false)
    private Instant collectedAt;
    @Column(name = "corrected_at")
    private Instant correctedAt;
    @Column(name = "data_revision", nullable = false)
    private Integer dataRevision;

    public static EodStockPrice of(Stock stock, java.time.LocalDate tradeDate, String quoteScope,
            BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal previousClose,
            Long volume, BigDecimal tradingAmount, Boolean noTrade, String sourceApi, String dataStatus) {
        if (stock == null || tradeDate == null || !"KRX".equals(quoteScope)
                || sourceApi == null || sourceApi.isBlank() || sourceApi.length() > 50
                || !java.util.Set.of("PROVISIONAL", "FINAL", "INCOMPLETE").contains(dataStatus)
                || (high != null && low != null && high.compareTo(low) < 0)
                || (volume != null && volume < 0) || negative(open) || negative(high) || negative(low)
                || negative(close) || negative(previousClose) || negative(tradingAmount)) {
            throw new IllegalArgumentException("Invalid EOD quote");
        }
        EodStockPrice value = new EodStockPrice();
        value.id = new EodStockPriceId(stock.getId(), tradeDate, quoteScope);
        value.stock = stock;
        value.openPrice = open;
        value.highPrice = high;
        value.lowPrice = low;
        value.closePrice = close;
        value.previousClosePrice = previousClose;
        value.volume = volume;
        value.tradingAmount = tradingAmount;
        value.noTrade = noTrade;
        value.sourceApi = sourceApi;
        value.dataStatus = dataStatus;
        value.collectedAt = Instant.now();
        value.dataRevision = 1;
        return value;
    }

    public boolean reviseFrom(EodStockPrice source, Instant now) {
        boolean changed = !same(openPrice, source.openPrice) || !same(highPrice, source.highPrice)
                || !same(lowPrice, source.lowPrice) || !same(closePrice, source.closePrice)
                || !same(previousClosePrice, source.previousClosePrice) || !Objects.equals(volume, source.volume)
                || !same(tradingAmount, source.tradingAmount) || !Objects.equals(noTrade, source.noTrade)
                || !Objects.equals(dataStatus, source.dataStatus) || !Objects.equals(sourceApi, source.sourceApi);
        collectedAt = now;
        if (changed) {
            openPrice = source.openPrice;
            highPrice = source.highPrice;
            lowPrice = source.lowPrice;
            closePrice = source.closePrice;
            previousClosePrice = source.previousClosePrice;
            volume = source.volume;
            tradingAmount = source.tradingAmount;
            noTrade = source.noTrade;
            dataStatus = source.dataStatus;
            sourceApi = source.sourceApi;
            dataRevision++;
            correctedAt = now;
        }
        return changed;
    }

    private static boolean same(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private static boolean negative(BigDecimal value) { return value != null && value.signum() < 0; }
}
