package org.stockwellness.batch.job.stock.sector.job.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.SectorApiDto;
import org.stockwellness.application.port.out.stock.SectorInsightPort;
import org.stockwellness.application.service.stock.SectorAnalysisService;
import org.stockwellness.domain.stock.insight.SectorInsight;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        return sectorAnalysisService.analyze(
                org.stockwellness.domain.stock.insight.MarketIndex.of(sectorCode, currentData.sectorName()),
                currentData,
                yesterdayData,
                pastPrices
        );
    }
}
