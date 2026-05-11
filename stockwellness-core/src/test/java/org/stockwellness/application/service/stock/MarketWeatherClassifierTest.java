package org.stockwellness.application.service.stock;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.application.port.in.stock.result.MarketWeatherLevel;
import org.stockwellness.application.port.in.stock.result.MarketWeatherResult;
import org.stockwellness.domain.stock.insight.MarketWeatherScore;
import static org.assertj.core.api.Assertions.assertThat;

class MarketWeatherClassifierTest {

    private final MarketWeatherFactory factory = new MarketWeatherFactory();
    private final MarketWeatherClassifier classifier = new MarketWeatherClassifier(factory);

    @Test
    @DisplayName("높은 점수는 CLEAR 상태를 반환한다")
    void shouldReturnClearForHighScores() {
        MarketWeatherScore score = new MarketWeatherScore(95, 95, 95, 95);
        MarketWeatherResult result = classifier.classify(score, LocalDate.now());
        
        assertThat(result.weatherLevel()).isEqualTo(MarketWeatherLevel.CLEAR);
    }

    @Test
    @DisplayName("낮은 점수는 STORMY 상태를 반환한다")
    void shouldReturnStormyForLowScores() {
        MarketWeatherScore score = new MarketWeatherScore(5, 5, 5, 5);
        MarketWeatherResult result = classifier.classify(score, LocalDate.now());
        
        assertThat(result.weatherLevel()).isEqualTo(MarketWeatherLevel.STORMY);
    }

    @Test
    @DisplayName("중간 점수는 CLOUDY 상태를 반환한다")
    void shouldReturnCloudyForMediumScores() {
        MarketWeatherScore score = new MarketWeatherScore(50, 50, 50, 50);
        MarketWeatherResult result = classifier.classify(score, LocalDate.now());
        
        assertThat(result.weatherLevel()).isEqualTo(MarketWeatherLevel.CLOUDY);
    }
}
