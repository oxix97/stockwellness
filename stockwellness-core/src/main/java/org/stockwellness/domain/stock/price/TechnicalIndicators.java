package org.stockwellness.domain.stock.price;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Embeddable
@NoArgsConstructor(access = PROTECTED) // JPA용 기본 생성자
@AllArgsConstructor
public class TechnicalIndicators implements Serializable {
    @Column(precision = 19, scale = 4)
    private BigDecimal ma5;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal ma20;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal ma60;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal ma120;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal rsi14;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal macd;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal macdSignal;

    // -- Bollinger Bands --
    @Column(name = "bollinger_upper", precision = 19, scale = 4)
    private BigDecimal bollingerUpper;

    @Column(name = "bollinger_mid", precision = 19, scale = 4)
    private BigDecimal bollingerMid; // Normally same as ma20

    @Column(name = "bollinger_lower", precision = 19, scale = 4)
    private BigDecimal bollingerLower;

    // -- ADX (Trend Strength) --
    @Column(name = "adx", precision = 19, scale = 4)
    private BigDecimal adx;

    @Column(name = "plus_di", precision = 19, scale = 4)
    private BigDecimal plusDi;

    @Column(name = "minus_di", precision = 19, scale = 4)
    private BigDecimal minusDi;

    // -- Status / Flags --
    @Enumerated(EnumType.STRING)
    @Column(name = "alignment_status", length = 20)
    private AlignmentStatus alignmentStatus;

    @Column(name = "is_golden_cross")
    private Boolean isGoldenCross;

    @Column(name = "is_dead_cross")
    private Boolean isDeadCross;

    @Column(name = "is_macd_cross")
    private Boolean isMacdCross;

    public static TechnicalIndicators empty() {
        return new TechnicalIndicators(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null);
    }
}
