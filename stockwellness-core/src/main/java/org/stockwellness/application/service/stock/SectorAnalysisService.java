package org.stockwellness.application.service.stock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.stockwellness.application.port.out.stock.SectorApiDto;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.analysis.TechnicalCalculator;
import org.stockwellness.domain.stock.analysis.TechnicalIndicatorCalculator;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SectorAnalysisService {

    public SectorInsight analyze(MarketIndex index, SectorApiDto currentData, SectorInsight yesterdayData, List<BigDecimal> pastPrices) {
        String sectorCode = index.getIndexCode();
        
        // 1. 수급 지표 연산 (연속 매수 일수)
        int foreignConsecutiveDays = calculateConsecutiveDays(
                currentData.netForeignBuyAmount(),
                yesterdayData != null ? yesterdayData.getForeignConsecutiveBuyDays() : 0
        );
        int instConsecutiveDays = calculateConsecutiveDays(
                currentData.netInstBuyAmount(),
                yesterdayData != null ? yesterdayData.getInstConsecutiveBuyDays() : 0
        );

        // 2. 기술적 지표 연산
        TechnicalIndicators calculated;
        boolean isOverheated = false;

        if (pastPrices == null || pastPrices.isEmpty()) {
            log.warn("섹터 {}의 과거 시세 데이터가 없어 지표 계산을 건너뜁니다.", sectorCode);
            calculated = TechnicalIndicators.empty();
        } else {
            List<BigDecimal> closingPricesForQuant = new ArrayList<>(pastPrices);
            closingPricesForQuant.add(currentData.sectorIndexCurrentPrice());
            calculated = TechnicalIndicatorCalculator.calculateLatest(closingPricesForQuant);
            
            isOverheated = TechnicalCalculator.isOverheated(
                    currentData.sectorIndexCurrentPrice(),
                    calculated.getMa20(),
                    calculated.getRsi14()
            );
        }

        // 3. 최종 Entity 생성
        return SectorInsight.of(
                currentData.sectorName(),
                sectorCode,
                resolveMarketType(sectorCode),
                currentData.baseDate(),
                currentData.sectorIndexCurrentPrice(),
                currentData.avgFluctuationRate(),
                currentData.netForeignBuyAmount(),
                currentData.netInstBuyAmount(),
                foreignConsecutiveDays,
                instConsecutiveDays,
                calculated,
                isOverheated
        );
    }

    private int calculateConsecutiveDays(Long todayNetBuyAmount, int yesterdayConsecutiveDays) {
        if (todayNetBuyAmount != null && todayNetBuyAmount > 0) {
            return yesterdayConsecutiveDays + 1;
        }
        return 0;
    }

    private MarketType resolveMarketType(String indexCode) {
        if (indexCode != null && (indexCode.startsWith("0") || indexCode.startsWith("2"))) return MarketType.KOSPI;
        if (indexCode != null && indexCode.startsWith("1")) return MarketType.KOSDAQ;
        return MarketType.KOSPI;
    }
}
