package org.stockwellness.domain.stock.price;

import org.stockwellness.domain.stock.Stock;
import com.querydsl.core.annotations.QueryTransient;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    // [최종] DB 컬럼명과 Java 필드명을 완전히 일치시켜 QueryDSL/JPA 충돌 방지
    @Column(name = "transaction_amt", precision = 25, scale = 2)
    private BigDecimal transactionAmt;

    @Embedded
    private TechnicalIndicators indicators;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
                .divide(base, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public static StockPrice of(Stock stock, LocalDate baseDate, BigDecimal open, BigDecimal high, BigDecimal low,
                                BigDecimal close, BigDecimal adjClose, BigDecimal previousClose,
                                Long volume, BigDecimal transactionAmt, TechnicalIndicators indicators) {
        var entity = new StockPrice();
        // [중요] StockPriceId 생성 시 baseDate 가 첫 번째 파라미터가 되도록 수정
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
        entity.indicators = indicators;
        return entity;
    }
}
