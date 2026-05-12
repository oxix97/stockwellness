package org.stockwellness.application.service.stock;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.stock.result.MarketWeatherLevel;
import org.stockwellness.application.port.in.stock.result.MarketWeatherReason;
import org.stockwellness.application.port.in.stock.result.MarketWeatherResult;
import org.stockwellness.domain.stock.insight.MarketWeatherScore;

@Component
@RequiredArgsConstructor
public class MarketWeatherClassifier {

    private final MarketWeatherFactory marketWeatherFactory;

    /**
     * 통합 점수(Integrated Score)를 기반으로 7단계 시장 기상 상태를 결정합니다.
     */
    public MarketWeatherResult classify(MarketWeatherScore score, LocalDate asOfDate) {
        int integratedScore = score.integratedScore();

        if (integratedScore >= 90) {
            return marketWeatherFactory.create(MarketWeatherLevel.CLEAR, MarketWeatherReason.BROAD_RALLY, asOfDate);
        }
        if (integratedScore >= 75) {
            return marketWeatherFactory.create(MarketWeatherLevel.SUNNY, MarketWeatherReason.STEADY_ADVANCE, asOfDate);
        }
        if (integratedScore >= 60) {
            return marketWeatherFactory.create(MarketWeatherLevel.PARTLY_CLOUDY, MarketWeatherReason.NARROW_ADVANCE, asOfDate);
        }
        if (integratedScore >= 40) {
            return marketWeatherFactory.create(MarketWeatherLevel.CLOUDY, MarketWeatherReason.SIDEWAYS, asOfDate);
        }
        if (integratedScore >= 25) {
            return marketWeatherFactory.create(MarketWeatherLevel.FOGGY, MarketWeatherReason.HIDDEN_WEAKNESS, asOfDate);
        }
        if (integratedScore >= 10) {
            return marketWeatherFactory.create(MarketWeatherLevel.RAINY, MarketWeatherReason.BROAD_SELL_OFF, asOfDate);
        }
        return marketWeatherFactory.create(MarketWeatherLevel.STORMY, MarketWeatherReason.VOLATILE_SELL_OFF, asOfDate);
    }
}
