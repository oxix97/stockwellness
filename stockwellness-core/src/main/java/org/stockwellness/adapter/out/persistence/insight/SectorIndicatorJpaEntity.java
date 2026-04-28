package org.stockwellness.adapter.out.persistence.insight;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.domain.shared.AbstractEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Entity
@Table(name = "sector_indicator")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SectorIndicatorJpaEntity extends AbstractEntity {

    @Column(nullable = false)
    private LocalDate baseDate;

    @Column(nullable = false, length = 20)
    private String sectorCode;

    @Column(precision = 10, scale = 4)
    private BigDecimal ma20;

    @Column(precision = 10, scale = 4)
    private BigDecimal ma60;

    @Column(precision = 10, scale = 4)
    private BigDecimal rsi14;

    @Column(precision = 10, scale = 4)
    private BigDecimal macd;

    @Column(precision = 10, scale = 4)
    private BigDecimal adr;

    private boolean isOverheated = false;

    @Builder
    public SectorIndicatorJpaEntity(LocalDate baseDate, String sectorCode, BigDecimal ma20, BigDecimal ma60, BigDecimal rsi14, BigDecimal macd, BigDecimal adr, boolean isOverheated) {
        this.baseDate = baseDate;
        this.sectorCode = sectorCode;
        this.ma20 = ma20;
        this.ma60 = ma60;
        this.rsi14 = rsi14;
        this.macd = macd;
        this.adr = adr;
        this.isOverheated = isOverheated;
    }
}
