package org.stockwellness.domain.stock;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Embeddable
@NoArgsConstructor(access = PROTECTED) // JPA용 기본 생성자
@AllArgsConstructor
public class TechnicalIndicators {
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

    public static TechnicalIndicators empty() {
        return new TechnicalIndicators(null, null, null, null, null, null, null);
    }
}
