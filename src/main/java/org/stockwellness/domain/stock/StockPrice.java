package org.stockwellness.domain.stock;

import com.querydsl.core.annotations.QueryTransient;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Entity
@Table(
        name = "stock_price",
        indexes = {
                @Index(name = "idx_stock_price_lookup", columnList = "stock_id, base_date DESC"),
                @Index(name = "idx_stock_price_date_amt", columnList = "base_date, transaction_amt DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class StockPrice {

    @EmbeddedId
    private StockPriceId id;

    @ManyToOne(fetch = LAZY)
    @MapsId("stockId")
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    // --- OHLCV Data (직접 필드) ---
    @Column(precision = 19, scale = 4)
    private BigDecimal openPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal highPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal lowPrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal closePrice;

    @Column(name = "adj_close_price", precision = 19, scale = 4)
    private BigDecimal adjClosePrice; // 수정종가

    private Long volume;

    @Column(name = "transaction_amt", precision = 25, scale = 2)
    private BigDecimal transactionAmount; // 거래대금

    // --- Embedded Value Object (기술적 지표) ---
    @Embedded
    @QueryTransient // QueryDSL 분석 대상에서 제외
    private TechnicalIndicators indicators;

    @CreatedDate
    @DateTimeFormat(iso = DATE_TIME)
    @Column(name = "created_at",updatable = false)
    private LocalDateTime createdAt;

    public static StockPrice of(
            Stock stock,
            LocalDate baseDate, // [추가] 날짜를 인자로 받아야 ID 생성 가능
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal adjClose,
            Long volume,
            BigDecimal transactionAmount,
            TechnicalIndicators indicators
    ) {
        StockPrice entity = new StockPrice();
        entity.id = new StockPriceId(stock.getId(), baseDate);
        entity.stock = stock;
        entity.openPrice = open;
        entity.highPrice = high;
        entity.lowPrice = low;
        entity.closePrice = close;
        entity.adjClosePrice = adjClose;
        entity.volume = volume;
        entity.transactionAmount = transactionAmount;
        entity.indicators = indicators;

        return entity;
    }
}
