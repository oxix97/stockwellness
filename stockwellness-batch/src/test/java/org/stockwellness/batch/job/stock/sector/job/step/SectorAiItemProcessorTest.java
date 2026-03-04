package org.stockwellness.batch.job.stock.sector.job.step;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.application.port.out.sector.LoadSectorAiPort;
import org.stockwellness.application.port.out.sector.SectorAiContext;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.analysis.AiReport;
import org.stockwellness.domain.stock.analysis.InvestmentDecision;
import org.stockwellness.domain.stock.insight.SectorIndicators;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SectorAiItemProcessorTest {

    private final LoadSectorAiPort loadSectorAiPort = mock(LoadSectorAiPort.class);
    private final SectorAiItemProcessor processor = new SectorAiItemProcessor(loadSectorAiPort);

    @Test
    @DisplayName("기술적 지표에 따른 TrendStatus(정배열) 판별 검증")
    void resolveTrendStatusRegular() {
        // given
        TechnicalIndicators technical = mock(TechnicalIndicators.class);
        when(technical.getMa5()).thenReturn(new BigDecimal("1200"));
        when(technical.getMa20()).thenReturn(new BigDecimal("1100"));
        when(technical.getMa60()).thenReturn(new BigDecimal("1000"));
        when(technical.getRsi14()).thenReturn(new BigDecimal("60"));

        SectorInsight insight = SectorInsight.of(
                "테스트", "001", MarketType.KOSPI, LocalDate.now(),
                SectorIndicators.of(BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, 0, 0),
                technical, false
        );

        AiReport report = new AiReport(InvestmentDecision.BUY, 90, "제목", List.of("이유"), "내용");
        when(loadSectorAiPort.generateSectorOpinion(any(SectorAiContext.class))).thenReturn(report);

        // when
        SectorInsight result = processor.process(insight);

        // then
        assertThat(result.getAiOpinion()).isNotNull();
        assertThat(result.getAiOpinion().getDecision()).isEqualTo(InvestmentDecision.BUY);
    }

    @Test
    @DisplayName("AI 분석 결과가 SectorInsight에 임베디드 타입으로 업데이트되는지 확인")
    void processUpdateOpinion() {
        // given
        SectorInsight insight = createDefaultInsight();
        AiReport report = new AiReport(
                InvestmentDecision.HOLD,
                70,
                "중립 의견",
                List.of("시장 불확실성"),
                "상세 리포트"
        );
        when(loadSectorAiPort.generateSectorOpinion(any(SectorAiContext.class))).thenReturn(report);

        // when
        SectorInsight result = processor.process(insight);

        // then
        assertThat(result.getAiOpinion()).isNotNull();
        assertThat(result.getAiOpinion().getDecision()).isEqualTo(InvestmentDecision.HOLD);
        assertThat(result.getAiOpinion().getTitle()).isEqualTo("중립 의견");
        assertThat(result.getAiOpinion().getConfidenceScore()).isEqualTo(70);
    }

    private SectorInsight createDefaultInsight() {
        return SectorInsight.of(
                "테스트", "001", MarketType.KOSPI, LocalDate.now(),
                SectorIndicators.of(BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, 0, 0),
                TechnicalIndicators.empty(),
                false
        );
    }
}
