package org.stockwellness.domain.stock.insight;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.stockwellness.domain.shared.AbstractEntity;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(
        name = "sector_insight",
        uniqueConstraints = {@UniqueConstraint(name = "uk_sector_insight", columnNames = {"sector_code", "base_date"})},
        indexes = {@Index(name = "idx_base_date", columnList = "base_date")}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SectorInsight extends AbstractEntity {

    @Column(nullable = false, length = 10)
    private String sectorCode;

    @Column(nullable = false)
    private LocalDate baseDate;

    // [시세 지표]
    @Column(precision = 10, scale = 2)
    private BigDecimal sectorIndexCurrentPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal avgFluctuationRate;

    // [수급 지표] 연속 매수 일수 포함
    private Long netForeignBuyAmount;
    private Long netInstBuyAmount;
    private Integer foreignConsecutiveBuyDays = 0;
    private Integer instConsecutiveBuyDays = 0;

    @Embedded
    private TechnicalIndicators technicalIndicators;

    // [주도주]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<LeadingStock> leadingStocks = new ArrayList<>();

    public static SectorInsight of(
            String sectorCode,
            LocalDate baseDate,
            BigDecimal sectorIndexCurrentPrice,
            BigDecimal avgFluctuationRate,
            Long netForeignBuyAmount,
            Long netInstBuyAmount,
            Integer foreignConsecutiveBuyDays,
            Integer instConsecutiveBuyDays,
            TechnicalIndicators technicalIndicators
    ) {
        var entity = new SectorInsight();
        entity.sectorCode = sectorCode;
        entity.baseDate = baseDate;
        entity.sectorIndexCurrentPrice = sectorIndexCurrentPrice;
        entity.avgFluctuationRate = avgFluctuationRate;
        entity.netForeignBuyAmount = netForeignBuyAmount;
        entity.netInstBuyAmount = netInstBuyAmount;
        entity.foreignConsecutiveBuyDays = foreignConsecutiveBuyDays;
        entity.instConsecutiveBuyDays = instConsecutiveBuyDays;
        entity.technicalIndicators = technicalIndicators;
        return entity;
    }
}