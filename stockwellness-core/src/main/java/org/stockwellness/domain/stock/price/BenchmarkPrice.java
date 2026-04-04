package org.stockwellness.domain.stock.price;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.domain.shared.AbstractEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

import static lombok.AccessLevel.PROTECTED;

/**
 * 주요 시장 지수(Benchmark)의 일별 시세를 저장하는 엔티티
 */
@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(
        name = "benchmark_price",
        indexes = {
                @Index(name = "idx_benchmark_ticker_date", columnList = "ticker, base_date", unique = true),
                @Index(name = "idx_benchmark_date", columnList = "base_date")
        }
)
public class BenchmarkPrice extends AbstractEntity {

    @Column(nullable = false, length = 30)
    private String name;

    /**
     * 지수 티커 / 코드 (e.g. "KOSPI", "KOSDAQ", "2001")
     */
    @Column(nullable = false, length = 20)
    private String ticker;

    /**
     * 기준 일자
     */
    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    /**
     * 시가
     */
    @Column(precision = 19, scale = 4)
    private BigDecimal openPrice;

    /**
     * 고가
     */
    @Column(precision = 19, scale = 4)
    private BigDecimal highPrice;

    /**
     * 저가
     */
    @Column(precision = 19, scale = 4)
    private BigDecimal lowPrice;

    /**
     * 종가 (지수 포인트)
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal closePrice;

    /**
     * 전일 대비 등락률 (%)
     */
    @Column(precision = 10, scale = 4)
    private BigDecimal changeRate;

    /**
     * 거래량 (지수 거래량 또는 관련 ETF 거래량 대용)
     */
    @Column
    private Long volume;

    public static BenchmarkPrice of(String name, String ticker, LocalDate baseDate, BigDecimal closePrice) {
        BenchmarkPrice bp = new BenchmarkPrice();
        bp.name = name;
        bp.ticker = ticker;
        bp.baseDate = baseDate;
        bp.closePrice = closePrice;
        return bp;
    }

    public void updatePrices(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal rate, Long vol) {
        this.openPrice = open;
        this.highPrice = high;
        this.lowPrice = low;
        this.closePrice = close;
        this.changeRate = rate;
        this.volume = vol;
    }
}
