package org.stockwellness.domain.stock.insight;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Embeddable
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SectorIndicators {

    // [시세 지표]
    @Column(name = "sector_index_current_price", precision = 10, scale = 2)
    private BigDecimal sectorIndexCurrentPrice;

    @Column(name = "avg_fluctuation_rate", precision = 10, scale = 2)
    private BigDecimal avgFluctuationRate;

    // [수급 지표]
    @Column(name = "net_foreign_buy_amount")
    private Long netForeignBuyAmount;

    @Column(name = "net_inst_buy_amount")
    private Long netInstBuyAmount;

    @Column(name = "foreign_consecutive_buy_days")
    private Integer foreignConsecutiveBuyDays = 0;

    @Column(name = "inst_consecutive_buy_days")
    private Integer instConsecutiveBuyDays = 0;

    public static SectorIndicators of(
            BigDecimal sectorIndexCurrentPrice,
            BigDecimal avgFluctuationRate,
            Long netForeignBuyAmount,
            Long netInstBuyAmount,
            Integer foreignConsecutiveBuyDays,
            Integer instConsecutiveBuyDays
    ) {
        return new SectorIndicators(
                sectorIndexCurrentPrice,
                avgFluctuationRate,
                netForeignBuyAmount,
                netInstBuyAmount,
                foreignConsecutiveBuyDays,
                instConsecutiveBuyDays
        );
    }
}
