package org.stockwellness.domain.stock;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

import static lombok.AccessLevel.PROTECTED;

/**
 * 주식 일별 시세 엔티티
 * <p>
 * 금융위원회 주식시세정보 API의 시계열 데이터(Time-Series)를 저장합니다.
 * 복합키(ISIN + 기준일자)를 사용하여 특정 종목의 일자별 조회를 최적화합니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(name = "stock_history", indexes = {
        @Index(name = "idx_stock_history_calc", columnList = "isin_code, base_date DESC")
})
@IdClass(StockHistoryId.class)
public class StockHistory {

    /**
     * ISIN 코드 (FK 성격)
     * Source: isinCd
     */
    @Id
    @Column(name = "isin_code", length = 12, nullable = false)
    private String isinCode;

    /**
     * 기준 일자
     * Source: basDt
     * Format: YYYYMMDD -> LocalDate 변환
     */
    @Id
    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    /**
     * 종가 (Close Price)
     * Source: clpr
     */
    @Column(name = "close_price", precision = 19, scale = 2)
    private BigDecimal closePrice;

    /**
     * 시가 (Open Price)
     * Source: mkp
     */
    @Column(name = "open_price", precision = 19, scale = 2)
    private BigDecimal openPrice;

    /**
     * 고가 (High Price)
     * Source: hipr
     */
    @Column(name = "high_price", precision = 19, scale = 2)
    private BigDecimal highPrice;

    /**
     * 저가 (Low Price)
     * Source: lopr
     */
    @Column(name = "low_price", precision = 19, scale = 2)
    private BigDecimal lowPrice;

    /**
     * 대비 (전일 대비 등락폭)
     * Source: vs
     */
    @Column(name = "price_change", precision = 19, scale = 2)
    private BigDecimal priceChange;

    /**
     * 등락률 (Fluctuation Rate)
     * Source: fltRt
     * Example: -4.57 -> -4.57%
     */
    @Column(name = "fluctuation_rate", precision = 10, scale = 4)
    private BigDecimal fluctuationRate;

    /**
     * 거래량 (Trading Volume)
     * Source: trqu
     * Note: 주식 수는 정수이므로 Long 사용
     */
    @Column(name = "volume")
    private Long volume;

    /**
     * 거래대금 (Trading Value)
     * Source: trPrc
     * Note: 거래량 * 가격이므로 금액이 매우 커질 수 있어 BigDecimal 권장
     */
    @Column(name = "trading_value", precision = 25, scale = 2)
    private BigDecimal tradingValue;

    /**
     * 시가총액 (Market Capitalization)
     * Source: mrktTotAmt
     * Note: 삼성전자 등 대형주는 조(Trillion) 단위이므로 BigDecimal 사용
     */
    @Column(name = "market_cap", precision = 25, scale = 2)
    private BigDecimal marketCap;

    // API에는 없지만, 우리 프로젝트의 AI 예측 전략을 위해 배치 시 계산하여 저장하는 필드들
    /**
     * 5일 이동평균선
     */
    @Column(name = "ma_5", precision = 19, scale = 2)
    private BigDecimal ma5;

    /**
     * 20일 이동평균선
     */
    @Column(name = "ma_20", precision = 19, scale = 2)
    private BigDecimal ma20;

    /**
     * RSI (14일 기준)
     */
    @Column(name = "rsi_14", precision = 10, scale = 4)
    private BigDecimal rsi14;

    @Column(precision = 19, scale = 4)
    private BigDecimal macd;  // MACD (12, 26)

    public static StockHistory create(
            String isinCode, LocalDate baseDate,
            BigDecimal closePrice, BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice,
            BigDecimal priceChange, BigDecimal fluctuationRate,
            Long volume, BigDecimal tradingValue, BigDecimal marketCap) {

        StockHistory history = new StockHistory();
        history.isinCode = isinCode;
        history.baseDate = baseDate;
        history.closePrice = closePrice;
        history.openPrice = openPrice;
        history.highPrice = highPrice;
        history.lowPrice = lowPrice;
        history.priceChange = priceChange;
        history.fluctuationRate = fluctuationRate;
        history.volume = volume;
        history.tradingValue = tradingValue;
        history.marketCap = marketCap;
        return history;
    }

    public void updateMa5(BigDecimal ma5) {
        this.ma5 = ma5;
    }

    public void updateMa20(BigDecimal ma20) {
        this.ma20 = ma20;
    }

    public void updateRsi14(BigDecimal rsi14) {
        this.rsi14 = rsi14;
    }

    public void updateMacd(BigDecimal macd) {
        this.macd = macd;
    }

    public void updateIndicators(TechnicalIndicators indicators) {
        if (indicators == null) return;

        this.ma5 = indicators.ma5();
        this.ma20 = indicators.ma20();
        this.rsi14 = indicators.rsi14();
        this.macd = indicators.macd();
        // Audit 용으로 updated_at 갱신이 필요하다면 여기서 처리하거나 @PreUpdate 활용
        // this.updatedAt = LocalDate.now();
    }
}