package org.stockwellness.domain.stock.insight;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Getter
@Embeddable
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PRIVATE)
public class SectorTechnicalIndicators {

    @Column(precision = 19, scale = 4)
    private BigDecimal ma5;

    @Column(precision = 19, scale = 4)
    private BigDecimal ma20;

    @Column(precision = 19, scale = 4)
    private BigDecimal ma60;

    @Column(precision = 19, scale = 4)
    private BigDecimal ma120;

    @Column(precision = 9, scale = 4)
    private BigDecimal rsi14;

    @Column(precision = 19, scale = 4)
    private BigDecimal macdLine;

    @Column(precision = 19, scale = 4)
    private BigDecimal signalLine;

    public static SectorTechnicalIndicators of(
            BigDecimal ma5, BigDecimal ma20, BigDecimal ma60, BigDecimal ma120,
            BigDecimal rsi14, BigDecimal macdLine, BigDecimal signalLine) {
        return new SectorTechnicalIndicators(ma5, ma20, ma60, ma120, rsi14, macdLine, signalLine);
    }
}