package org.stockwellness.domain.stock.insight;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.analysis.InvestmentDecision;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SectorInsightTest {

    @Test
    @DisplayName("SectorInsight 생성 및 Indicators 위임 메서드 검증")
    void createSectorInsight() {
        // given
        SectorIndicators indicators = SectorIndicators.of(
                new BigDecimal("2500.00"),
                new BigDecimal("1.50"),
                1000L,
                500L,
                3,
                2
        );
        TechnicalIndicators technical = TechnicalIndicators.empty();
        LocalDate now = LocalDate.now();

        // when
        SectorInsight insight = SectorInsight.of(
                "전기전자", "001", MarketType.KOSPI, now,
                indicators, technical, false
        );

        // then
        assertThat(insight.getSectorName()).isEqualTo("전기전자");
        assertThat(insight.getIndicators()).isEqualTo(indicators);
        // 위임 메서드 검증
        assertThat(insight.getSectorIndexCurrentPrice()).isEqualTo(new BigDecimal("2500.00"));
        assertThat(insight.getNetForeignBuyAmount()).isEqualTo(1000L);
        assertThat(insight.getForeignConsecutiveBuyDays()).isEqualTo(3);
    }

    @Test
    @DisplayName("AI 의견(AiOpinion) 업데이트 검증")
    void updateAiOpinion() {
        // given
        SectorInsight insight = createEmptyInsight();
        SectorAiOpinion opinion = SectorAiOpinion.of(
                InvestmentDecision.BUY,
                85,
                "강력 매수 신호",
                List.of("외인 수급 지속", "주도주 강세"),
                "상세 분석 내용"
        );

        // when
        insight.updateAiOpinion(opinion);

        // then
        assertThat(insight.getAiOpinion()).isNotNull();
        assertThat(insight.getAiOpinion().getDecision()).isEqualTo(InvestmentDecision.BUY);
        assertThat(insight.getAiOpinion().getTitle()).isEqualTo("강력 매수 신호");
    }

    private SectorInsight createEmptyInsight() {
        return SectorInsight.of(
                "테스트", "TEST", MarketType.KOSPI, LocalDate.now(),
                SectorIndicators.of(BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, 0, 0),
                TechnicalIndicators.empty(),
                false
        );
    }
}
