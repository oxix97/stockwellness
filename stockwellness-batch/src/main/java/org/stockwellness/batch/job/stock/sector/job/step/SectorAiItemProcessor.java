package org.stockwellness.batch.job.stock.sector.job.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.sector.LoadSectorAiPort;
import org.stockwellness.application.port.out.sector.SectorAiContext;
import org.stockwellness.domain.stock.analysis.AiReport;
import org.stockwellness.domain.stock.analysis.TrendStatus;
import org.stockwellness.domain.stock.insight.SectorAiOpinion;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class SectorAiItemProcessor implements ItemProcessor<SectorInsight, SectorInsight> {

    private final LoadSectorAiPort loadSectorAiPort;

    @Override
    public SectorInsight process(SectorInsight insight) {
        long startTime = System.currentTimeMillis();
        log.info(">>> 섹터 AI 분석 시작: {} ({})", insight.getSectorName(), insight.getSectorCode());

        try {
            // ... (중략) ...
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("<<< 섹터 AI 분석 완료: {} ({}). 소요시간: {}ms", 
                    insight.getSectorName(), insight.getSectorCode(), duration);
            
        } catch (Exception e) {
            log.error("❌ 섹터 AI 분석 실패: {} ({}). 오류: {}", 
                    insight.getSectorName(), insight.getSectorCode(), e.getMessage());
            
            // Fallback 로직 적용: 장애 상황 기록하여 배치 중단 방지
            AiReport fallback = AiReport.fallback();
            insight.updateAiOpinion(SectorAiOpinion.of(
                    fallback.decision(),
                    fallback.confidenceScore(),
                    fallback.title(),
                    fallback.keyReasons(),
                    fallback.detailedAnalysis()
            ));
        }

        return insight;
    }

    private TrendStatus resolveTrendStatus(TechnicalIndicators indicators) {
        if (indicators == null || indicators.getMa5() == null || indicators.getMa20() == null || indicators.getMa60() == null) {
            return TrendStatus.NEUTRAL;
        }

        BigDecimal ma5 = indicators.getMa5();
        BigDecimal ma20 = indicators.getMa20();
        BigDecimal ma60 = indicators.getMa60();

        if (ma5.compareTo(ma20) > 0 && ma20.compareTo(ma60) > 0) {
            return TrendStatus.REGULAR;
        } else if (ma5.compareTo(ma20) < 0 && ma20.compareTo(ma60) < 0) {
            return TrendStatus.INVERSE;
        }
        return TrendStatus.NEUTRAL;
    }
}
