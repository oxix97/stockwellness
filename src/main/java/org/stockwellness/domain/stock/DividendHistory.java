package org.stockwellness.domain.stock;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Entity
@Table(
        name = "dividend_history",
        uniqueConstraints = {
                // 한 종목의 같은 배당락일에 중복 데이터 방지
                @UniqueConstraint(
                        name = "uq_dividend_stock_date",
                        columnNames = {"stock_id", "ex_date"}
                )
        }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DividendHistory {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    // 배당 정보 조회 시 종목 정보까지 매번 필요하지 않으므로 LAZY 설정
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    // 배당락일 (권리 획득 기준일)
    @Column(name = "ex_date", nullable = false)
    private LocalDate exDate;

    // 실제 지급일 (예정 데이터일 경우 null 가능성 고려)
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    // 배당금 (정밀한 계산을 위해 BigDecimal 사용)
    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    // 통화 (Enum 활용 - KRW, USD 등)
    @Enumerated(STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    // 생성일시 (수정일시 불필요 - 역사적 데이터)
    @CreatedDate
    @DateTimeFormat(iso = DATE_TIME)
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static DividendHistory of(
            Stock stock,
            LocalDate exDate,
            LocalDate paymentDate,
            BigDecimal amount,
            Currency currency
    ) {
        DividendHistory dividendHistory = new DividendHistory();
        dividendHistory.stock = stock;
        dividendHistory.exDate = exDate;
        dividendHistory.paymentDate = paymentDate;
        dividendHistory.amount = amount;
        dividendHistory.currency = currency;
        return dividendHistory;
    }

    // --- Business Logic ---

    // 배당 수익률 계산 (배당락일 주가 기준)
    public BigDecimal calculateYield(BigDecimal stockPriceAtExDate) {
        if (stockPriceAtExDate == null || stockPriceAtExDate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        // (배당금 / 주가) * 100
        return this.amount.divide(stockPriceAtExDate, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}