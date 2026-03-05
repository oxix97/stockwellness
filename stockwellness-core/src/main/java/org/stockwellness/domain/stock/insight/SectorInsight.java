package org.stockwellness.domain.stock.insight;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.stockwellness.domain.shared.AbstractEntity;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

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

    @Column(nullable = false, length = 50)
    private String sectorName; // 예: "전기전자", "Information Technology"

    @Column(nullable = false, length = 10)
    private String sectorCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MarketType marketType; // KOSPI, KOSDAQ, S&P500 등

    @Column(nullable = false)
    private LocalDate baseDate;

    @Embedded
    private SectorIndicators indicators;

    @Embedded
    private TechnicalIndicators technicalIndicators;

    private boolean isOverheated = false;

    // [주도주]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<LeadingStock> leadingStocks = new ArrayList<>();

    @Embedded
    private SectorAiOpinion aiOpinion;

    public static SectorInsight of(
            String sectorName,
            String sectorCode,
            MarketType marketType,
            LocalDate baseDate,
            SectorIndicators indicators,
            TechnicalIndicators technicalIndicators,
            boolean isOverheated
    ) {
        var entity = new SectorInsight();
        entity.sectorName = sectorName;
        entity.sectorCode = sectorCode;
        entity.marketType = marketType;
        entity.baseDate = baseDate;
        entity.indicators = indicators;
        entity.technicalIndicators = technicalIndicators;
        entity.isOverheated = isOverheated;
        return entity;
    }

    public void updateLeadingStocks(List<LeadingStock> leadingStocks) {
        this.leadingStocks = new ArrayList<>(leadingStocks);
    }

    public void updateAiOpinion(SectorAiOpinion aiOpinion) {
        this.aiOpinion = aiOpinion;
    }

    public void update(
            SectorIndicators indicators,
            TechnicalIndicators technicalIndicators,
            boolean isOverheated
    ) {
        this.indicators = indicators;
        this.technicalIndicators = technicalIndicators;
        this.isOverheated = isOverheated;
    }

    // Delegation methods for backward compatibility/convenience
    public java.math.BigDecimal getSectorIndexCurrentPrice() {
        return indicators != null ? indicators.getSectorIndexCurrentPrice() : null;
    }

    public java.math.BigDecimal getAvgFluctuationRate() {
        return indicators != null ? indicators.getAvgFluctuationRate() : null;
    }

    public Long getNetForeignBuyAmount() {
        return indicators != null ? indicators.getNetForeignBuyAmount() : 0L;
    }

    public Long getNetInstBuyAmount() {
        return indicators != null ? indicators.getNetInstBuyAmount() : 0L;
    }

    public Integer getForeignConsecutiveBuyDays() {
        return indicators != null ? indicators.getForeignConsecutiveBuyDays() : 0;
    }

    public Integer getInstConsecutiveBuyDays() {
        return indicators != null ? indicators.getInstConsecutiveBuyDays() : 0;
    }
}
