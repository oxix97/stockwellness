package org.stockwellness.batch.job.stock.sector.job.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.SectorApiDto;
import org.stockwellness.application.port.out.stock.SectorInsightPort;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.analysis.TechnicalCalculator;
import org.stockwellness.domain.stock.analysis.TechnicalIndicatorCalculator;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SectorInsightItemProcessor implements ItemProcessor<SectorApiDto, SectorInsight> {

    private final SectorInsightPort sectorInsightPort;
    private final SectorAnalysisService sectorAnalysisService;

    @Override
    public SectorInsight process(SectorApiDto currentData) {
        LocalDate today = currentData.baseDate();
        String sectorCode = currentData.sectorCode();

        // 1. 도메인 메타데이터 로드
        SectorInsight yesterdayData = sectorInsightPort.findLatestBefore(sectorCode, today).orElse(null);
        List<BigDecimal> pastPrices = sectorInsightPort.findPastPrices(sectorCode, today, 119);

        // 2. 도메인 서비스로 위임 (분석 로직)
        // Note: MarketIndex index 가 현재 Processor 에는 없으므로, SectorApiDto 정보를 기반으로 임시 객체 생성 혹은 DTO 확장 필요.
        // 여기서는 기존 로직 유지를 위해 필요한 정보만 전달하거나 analyze 시그니처 조정.
        // 현재 analyze 는 MarketIndex 를 받으므로, SectorApiDto 에서 필요한 정보를 추출하여 MarketIndex.of 로 전달.
        return sectorAnalysisService.analyze(
                org.stockwellness.domain.stock.insight.MarketIndex.of(sectorCode, currentData.sectorName()),
                currentData,
                yesterdayData,
                pastPrices
        );
    }
}
