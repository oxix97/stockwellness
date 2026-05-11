package org.stockwellness.domain.stock.insight;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

/**
 * 섹터 전용 기술 지표 모델 (다이어트 버전)
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SectorTechnicalIndicators {

    @Column(precision = 19, scale = 4)
    private BigDecimal ma5;

    @Column(precision = 19, scale = 4)
    private BigDecimal ma20;

    @Column(precision = 19, scale = 4)
    private BigDecimal rsi14;

    @Column(name = "bollinger_upper", precision = 19, scale = 4)
    private BigDecimal bollingerUpper;

    @Column(name = "bollinger_mid", precision = 19, scale = 4)
    private BigDecimal bollingerMid;

    @Column(name = "bollinger_lower", precision = 19, scale = 4)
    private BigDecimal bollingerLower;

    @Column(name = "is_golden_cross")
    private Boolean isGoldenCross;

    @Column(name = "is_dead_cross")
    private Boolean isDeadCross;

    public static SectorTechnicalIndicators empty() {
        return new SectorTechnicalIndicators(null, null, null, null, null, null, null, null);
    }

    public static SectorTechnicalIndicators from(TechnicalIndicators ti) {
        if (ti == null) return empty();
        return new SectorTechnicalIndicators(
                ti.getMa5(),
                ti.getMa20(),
                ti.getRsi14(),
                ti.getBollingerUpper(),
                ti.getBollingerMid(),
                ti.getBollingerLower(),
                ti.getIsGoldenCross(),
                ti.getIsDeadCross()
        );
    }

    public static SectorTechnicalIndicators of(
            BigDecimal ma5,
            BigDecimal ma20,
            BigDecimal rsi14,
            BigDecimal bollingerUpper,
            BigDecimal bollingerMid,
            BigDecimal bollingerLower,
            Boolean isGoldenCross,
            Boolean isDeadCross
    ) {
        return new SectorTechnicalIndicators(
                ma5, ma20, rsi14, bollingerUpper, bollingerMid, bollingerLower, isGoldenCross, isDeadCross
        );
    }
}
