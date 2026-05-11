package org.stockwellness.domain.stock.insight;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MarketWeatherDomainTest {

    @Test
    @DisplayName("MarketWeatherIndicatorSet 데이터 저장 및 불변성 검증")
    void indicatorSetTest() {
        BigDecimal ma20 = new BigDecimal("105.5");
        BigDecimal adr = new BigDecimal("110.0");
        BigDecimal rsi = new BigDecimal("65.2");
        
        MarketWeatherIndicatorSet indicatorSet = new MarketWeatherIndicatorSet(ma20, adr, rsi);
        
        assertThat(indicatorSet.ma20Disparity()).isEqualTo(ma20);
        assertThat(indicatorSet.adr()).isEqualTo(adr);
        assertThat(indicatorSet.rsi14()).isEqualTo(rsi);
    }

    @Test
    @DisplayName("MarketWeatherPolicy 기본값 및 데이터 저장 검증")
    void policyTest() {
        MarketWeatherPolicy policy = MarketWeatherPolicy.DEFAULT;
        
        assertThat(policy.trendWeight()).isEqualTo(new BigDecimal("0.4"));
        assertThat(policy.breadthWeight()).isEqualTo(new BigDecimal("0.4"));
        assertThat(policy.momentumWeight()).isEqualTo(new BigDecimal("0.2"));
        assertThat(policy.rollingWindowDays()).isEqualTo(252);
    }

    @Test
    @DisplayName("MarketWeatherScore 데이터 저장 검증")
    void scoreTest() {
        MarketWeatherScore score = new MarketWeatherScore(80, 70, 60, 72);
        
        assertThat(score.trendScore()).isEqualTo(80);
        assertThat(score.breadthScore()).isEqualTo(70);
        assertThat(score.momentumScore()).isEqualTo(60);
        assertThat(score.integratedScore()).isEqualTo(72);
    }
}
