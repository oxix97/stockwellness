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
        log.info(">>> Starting AI Analysis for Sector: {} ({})", insight.getSectorName(), insight.getSectorCode());

        try {
            SectorAiContext context = new SectorAiContext(
                    insight.getSectorName(),
                    insight.getSectorCode(),
                    insight.getMarketType(),
                    insight.getBaseDate(),
                    insight.getSectorIndexCurrentPrice(),
                    insight.getAvgFluctuationRate(),
                    insight.getNetForeignBuyAmount(),
                    insight.getNetInstBuyAmount(),
                    insight.getForeignConsecutiveBuyDays(),
                    insight.getInstConsecutiveBuyDays(),
                    resolveTrendStatus(insight.getTechnicalIndicators()),
                    insight.getTechnicalIndicators().getRsi14(),
                    insight.isOverheated(),
                    insight.getLeadingStocks()
            );

            AiReport report = loadSectorAiPort.generateSectorOpinion(context);

            SectorAiOpinion opinion = SectorAiOpinion.of(
                    report.decision(),
                    report.confidenceScore(),
                    report.title(),
                    report.keyReasons(),
                    report.detailedAnalysis()
            );

            insight.updateAiOpinion(opinion);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("<<< Finished AI Analysis for Sector: {} ({}). Took {}ms", 
                    insight.getSectorName(), insight.getSectorCode(), duration);
            
        } catch (Exception e) {
            log.error("❌ AI Analysis failed for Sector: {} ({}). Error: {}", 
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
