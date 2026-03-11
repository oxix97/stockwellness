package org.stockwellness.adapter.out.external.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.application.port.out.portfolio.AdvisorAiContext;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.analysis.CrossoverSignal;
import org.stockwellness.domain.stock.analysis.TrendStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PromptTemplateMapper 단위 테스트")
class PromptTemplateMapperTest {

    private final PromptTemplateMapper mapper = new PromptTemplateMapper();

    @Test
    @DisplayName("AdvisorAiContext를 기반으로 AI 프롬프트를 정상적으로 생성한다")
    void toAdvisorPromptString_success() {
        // given
        AiAnalysisContext tech = new AiAnalysisContext(
                "AAPL", LocalDate.now(),
                new AiAnalysisContext.PriceSummary(new BigDecimal("150.0"), BigDecimal.ONE, new BigDecimal("1000000")),
                new AiAnalysisContext.TechnicalSignal(TrendStatus.REGULAR, new BigDecimal("60.0"), "중립", BigDecimal.ONE, CrossoverSignal.NONE, 
                        new BigDecimal("145.0"), new BigDecimal("140.0"), new BigDecimal("135.0"), new BigDecimal("130.0")),
                new AiAnalysisContext.PortfolioRisk(false, 0.0)
        );

        AdvisorAiContext context = new AdvisorAiContext(
                "내 포트폴리오",
                List.of(new AdvisorAiContext.HoldingInfo("AAPL", "애플", BigDecimal.TEN, new BigDecimal("150.0"), new BigDecimal("20.0"), new BigDecimal("25.0"), tech)),
                List.of(new AdvisorAiContext.MarketBenchmark("KOSPI", new BigDecimal("2500.0"), new BigDecimal("0.5")))
        );

        // when
        String prompt = mapper.toAdvisorPromptString(context);

        // then
        assertThat(prompt).contains("내 포트폴리오");
        assertThat(prompt).contains("KOSPI");
        assertThat(prompt).contains("애플 (AAPL)");
        assertThat(prompt).contains("비중: 현재 +20.00% (목표 +25.00%)");
    }
}
