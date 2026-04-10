package org.stockwellness.domain.stock.price;

import com.querydsl.core.annotations.QueryTransient;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.stockwellness.domain.stock.Stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "stock_price", indexes = {
        @Index(name = "idx_stock_price_lookup", columnList = "stock_id, base_date DESC"),
        @Index(name = "idx_stock_price_indicators", columnList = "base_date, alignment_status, rsi14")
})
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockPrice {

    @EmbeddedId
    private StockPriceId id;

    @MapsId("stockId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id")
    private Stock stock;

    @Column(precision = 19, scale = 4)
    private BigDecimal openPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal highPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal lowPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal closePrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal adjClosePrice;

    @Column(name = "prev_close_price", precision = 19, scale = 4)
    private BigDecimal previousClosePrice;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "transaction_amt", precision = 25, scale = 2)
    private BigDecimal transactionAmt;

    // --- 수급 데이터 (V15 리팩토링 반영) ---
    @Column(name = "inst_buying_amt", precision = 25, scale = 2)
    private BigDecimal netInstitutionalBuyingAmt;

    @Column(name = "frgn_buying_amt", precision = 25, scale = 2)
    private BigDecimal netForeignBuyingAmt;

    @Column(name = "total_net_amt", precision = 25, scale = 2)
    private BigDecimal netTotalBuyingAmt;

    @Column(name = "inst_buying_qty")
    private Long netInstitutionalBuyingQty;

    @Column(name = "frgn_buying_qty")
    private Long netForeignBuyingQty;

    @Column(name = "total_net_qty")
    private Long netTotalBuyingQty;

    @Embedded
    private TechnicalIndicators indicators;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void updateIndicators(TechnicalIndicators indicators) {
        this.indicators = indicators;
    }

    /**
     * 등락률 계산 (전일 종가 우선, 없으면 시가 대비)
     */
    @QueryTransient
    public BigDecimal getFluctuationRate() {
        BigDecimal base = (previousClosePrice != null && previousClosePrice.compareTo(BigDecimal.ZERO) > 0)
                ? previousClosePrice
                : openPrice;

        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return closePrice.subtract(base)
                .divide(base, new java.math.MathContext(16))
                .multiply(BigDecimal.valueOf(100));
    }

    public static StockPrice of(Stock stock, LocalDate baseDate, BigDecimal open, BigDecimal high, BigDecimal low,
                                BigDecimal close, BigDecimal adjClose, BigDecimal previousClose,
                                Long volume, BigDecimal transactionAmt,
                                BigDecimal netInstitutionalBuyingAmt, BigDecimal netForeignBuyingAmt,
                                TechnicalIndicators indicators) {
        return of(
                stock,
                baseDate,
                open,
                high,
                low,
                close,
                adjClose,
                previousClose,
                volume,
                transactionAmt,
                netInstitutionalBuyingAmt,
                netForeignBuyingAmt,
                0L,
                0L,
                indicators
        );
    }

    public static StockPrice of(Stock stock, LocalDate baseDate, BigDecimal open, BigDecimal high, BigDecimal low,
                                BigDecimal close, BigDecimal adjClose, BigDecimal previousClose,
                                Long volume, BigDecimal transactionAmt,
                                BigDecimal netInstitutionalBuyingAmt, BigDecimal netForeignBuyingAmt,
                                Long netInstitutionalBuyingQty, Long netForeignBuyingQty,
                                TechnicalIndicators indicators) {
        var entity = new StockPrice();
        entity.id = new StockPriceId(baseDate, stock.getId());
        entity.stock = stock;
        entity.openPrice = open;
        entity.highPrice = high;
        entity.lowPrice = low;
        entity.closePrice = close;
        entity.adjClosePrice = adjClose;
        entity.previousClosePrice = previousClose;
        entity.volume = volume;
        entity.transactionAmt = transactionAmt;
        entity.netInstitutionalBuyingAmt = netInstitutionalBuyingAmt != null ? netInstitutionalBuyingAmt : BigDecimal.ZERO;
        entity.netForeignBuyingAmt = netForeignBuyingAmt != null ? netForeignBuyingAmt : BigDecimal.ZERO;
        entity.netTotalBuyingAmt = entity.netInstitutionalBuyingAmt.add(entity.netForeignBuyingAmt);
        entity.netInstitutionalBuyingQty = netInstitutionalBuyingQty != null ? netInstitutionalBuyingQty : 0L;
        entity.netForeignBuyingQty = netForeignBuyingQty != null ? netForeignBuyingQty : 0L;
        entity.netTotalBuyingQty = entity.netInstitutionalBuyingQty + entity.netForeignBuyingQty;
        entity.indicators = indicators;
        return entity;
    }

    public static StockPrice of(Stock stock, LocalDate baseDate, BigDecimal open, BigDecimal high, BigDecimal low,
                                BigDecimal close, BigDecimal adjClose, BigDecimal previousClose,
                                Long volume, BigDecimal transactionAmt, TechnicalIndicators indicators) {
        return of(stock, baseDate, open, high, low, close, adjClose, previousClose, volume, transactionAmt,
                BigDecimal.ZERO, BigDecimal.ZERO, indicators);
    }
}
