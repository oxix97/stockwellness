package org.stockwellness.domain.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.domain.shared.AbstractEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SectorInsight extends AbstractEntity {

    @Column(nullable = false, length = 10)
    private String sectorCode;

    @Column(nullable = false)
    private LocalDate baseDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal avgFluctuationRate;

    private Long netForeignBuy;

    private Long netInstBuy;

    @Convert(converter = LeadingStocksConverter.class)
    @Column(name = "leading_stocks", columnDefinition = "jsonb")
    private List<LeadingStock> leadingStocks = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String aiMarketSummary;


}