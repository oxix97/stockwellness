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

    @Column(nullable = false, length = 50)
    private String sectorName; // 예: "전기전자", "Information Technology"

    @Column(nullable = false, length = 10)
    private String sectorCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MarketType marketType; // KOSPI, KOSDAQ, S&P500 등

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

    private boolean isOverheated = false;

    // [주도주]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<LeadingStock> leadingStocks = new ArrayList<>();

    // MEMO : 추후 AI 기능 구현시 추가 될 내용들.
//    @Column(columnDefinition = "text")
//    private String aiSummary; // AI가 요약한 1~2줄 브리핑 (예: "외국인 수급이 5일째 유입되며 과열 양상이나, 반도체 주도주 중심으로 상승 여력 존재")

//    @Column(columnDefinition = "jsonb")
//    @JdbcTypeCode(SqlTypes.JSON)
//    private AiSectorAnalysis aiAnalysis; // 상세 분석 데이터 (점수, 긍정/부정 요인 등 구조화된 데이터)

        public static SectorInsight of(
                String sectorName,
                String sectorCode,
                MarketType marketType,
                LocalDate baseDate,
                BigDecimal sectorIndexCurrentPrice,
                BigDecimal avgFluctuationRate,
                Long netForeignBuyAmount,
                Long netInstBuyAmount,
                Integer foreignConsecutiveBuyDays,
                            Integer instConsecutiveBuyDays,
                            TechnicalIndicators technicalIndicators,
                            boolean isOverheated
                    ) {
                        var entity = new SectorInsight();
                        entity.sectorName = sectorName;
                        entity.sectorCode = sectorCode;
                        entity.marketType = marketType;
                        entity.baseDate = baseDate;
                        entity.sectorIndexCurrentPrice = sectorIndexCurrentPrice;
                        entity.avgFluctuationRate = avgFluctuationRate;
                        entity.netForeignBuyAmount = netForeignBuyAmount;
                        entity.netInstBuyAmount = netInstBuyAmount;
                        entity.foreignConsecutiveBuyDays = foreignConsecutiveBuyDays;
                        entity.instConsecutiveBuyDays = instConsecutiveBuyDays;
                        entity.technicalIndicators = technicalIndicators;
                        entity.isOverheated = isOverheated;
                        return entity;
                    }
                
                    public void updateLeadingStocks(List<LeadingStock> leadingStocks) {
                        this.leadingStocks = new ArrayList<>(leadingStocks);
                    }
                
                    public void update(
                            BigDecimal sectorIndexCurrentPrice,
                            BigDecimal avgFluctuationRate,
                            Long netForeignBuyAmount,
                            Long netInstBuyAmount,
                            Integer foreignConsecutiveBuyDays,
                            Integer instConsecutiveBuyDays,
                            TechnicalIndicators technicalIndicators,
                            boolean isOverheated
                    ) {
                        this.sectorIndexCurrentPrice = sectorIndexCurrentPrice;
                        this.avgFluctuationRate = avgFluctuationRate;
                        this.netForeignBuyAmount = netForeignBuyAmount;
                        this.netInstBuyAmount = netInstBuyAmount;
                        this.foreignConsecutiveBuyDays = foreignConsecutiveBuyDays;
                        this.instConsecutiveBuyDays = instConsecutiveBuyDays;
                        this.technicalIndicators = technicalIndicators;
                        this.isOverheated = isOverheated;
                    }
                }
                