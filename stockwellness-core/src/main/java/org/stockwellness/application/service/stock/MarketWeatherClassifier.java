package org.stockwellness.application.service.stock;

import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.stock.result.MarketWeatherLevel;
import org.stockwellness.application.port.in.stock.result.MarketWeatherReason;
import org.stockwellness.application.port.in.stock.result.MarketWeatherResult;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class MarketWeatherClassifier {

    private final MarketWeatherFactory marketWeatherFactory;

    public MarketWeatherClassifier(MarketWeatherFactory marketWeatherFactory) {
        this.marketWeatherFactory = marketWeatherFactory;
    }

    public MarketWeatherResult classify(
            BigDecimal kospiRate,
            BigDecimal kosdaqRate,
            MarketBreadthSnapshot breadth,
            LocalDate asOfDate
    ) {
        if (breadth == null) {
            return classifyWithIndexOnly(kospiRate, kosdaqRate, asOfDate);
        }

        if ((kospiRate.compareTo(MarketWeatherPolicy.STORMY_KOSPI) <= 0
                || kosdaqRate.compareTo(MarketWeatherPolicy.STORMY_KOSDAQ) <= 0
                || breadth.declineRatio().compareTo(MarketWeatherPolicy.STORMY_DECLINE_RATIO) >= 0)
                && breadth.highVolatilityRatio().compareTo(MarketWeatherPolicy.STORMY_HIGH_VOLATILITY_RATIO) >= 0) {
            return marketWeatherFactory.create(MarketWeatherLevel.STORMY, MarketWeatherReason.VOLATILE_SELL_OFF, asOfDate);
        }

        if (kospiRate.compareTo(MarketWeatherPolicy.CLEAR_KOSPI) >= 0
                && kosdaqRate.compareTo(MarketWeatherPolicy.CLEAR_KOSDAQ) >= 0
                && breadth.advanceRatio().compareTo(MarketWeatherPolicy.CLEAR_ADVANCE_RATIO) >= 0
                && breadth.highVolatilityRatio().compareTo(MarketWeatherPolicy.CLEAR_HIGH_VOLATILITY_RATIO_CAP) < 0) {
            return marketWeatherFactory.create(MarketWeatherLevel.CLEAR, MarketWeatherReason.BROAD_RALLY, asOfDate);
        }

        if (kospiRate.compareTo(MarketWeatherPolicy.SUNNY_KOSPI) >= 0
                && kosdaqRate.compareTo(MarketWeatherPolicy.SUNNY_KOSDAQ) >= 0
                && breadth.advanceRatio().compareTo(MarketWeatherPolicy.SUNNY_ADVANCE_RATIO) >= 0) {
            return marketWeatherFactory.create(MarketWeatherLevel.SUNNY, MarketWeatherReason.STEADY_ADVANCE, asOfDate);
        }

        if ((kospiRate.compareTo(MarketWeatherPolicy.PARTLY_CLOUDY_INDEX) >= 0
                || kosdaqRate.compareTo(MarketWeatherPolicy.PARTLY_CLOUDY_INDEX) >= 0)
                && breadth.advanceRatio().compareTo(MarketWeatherPolicy.PARTLY_CLOUDY_ADVANCE_RATIO) >= 0) {
            return marketWeatherFactory.create(MarketWeatherLevel.PARTLY_CLOUDY, MarketWeatherReason.NARROW_ADVANCE, asOfDate);
        }

        if ((kospiRate.compareTo(MarketWeatherPolicy.FOGGY_KOSPI_FLOOR) >= 0
                && breadth.declineRatio().compareTo(MarketWeatherPolicy.FOGGY_DECLINE_RATIO) >= 0)
                || (kospiRate.compareTo(BigDecimal.ZERO) >= 0
                && kosdaqRate.compareTo(MarketWeatherPolicy.FOGGY_KOSDAQ) <= 0
                && breadth.declineRatio().compareTo(MarketWeatherPolicy.FOGGY_ALT_DECLINE_RATIO) >= 0)) {
            return marketWeatherFactory.create(MarketWeatherLevel.FOGGY, MarketWeatherReason.HIDDEN_WEAKNESS, asOfDate);
        }

        if (kospiRate.compareTo(MarketWeatherPolicy.RAINY_KOSPI) <= 0
                || kosdaqRate.compareTo(MarketWeatherPolicy.RAINY_KOSDAQ) <= 0
                || breadth.declineRatio().compareTo(MarketWeatherPolicy.RAINY_DECLINE_RATIO) >= 0) {
            return marketWeatherFactory.create(MarketWeatherLevel.RAINY, MarketWeatherReason.BROAD_SELL_OFF, asOfDate);
        }

        return marketWeatherFactory.create(MarketWeatherLevel.CLOUDY, MarketWeatherReason.SIDEWAYS, asOfDate);
    }

    private MarketWeatherResult classifyWithIndexOnly(BigDecimal kospiRate, BigDecimal kosdaqRate, LocalDate asOfDate) {
        if (kospiRate.compareTo(MarketWeatherPolicy.INDEX_ONLY_CLEAR_KOSPI) >= 0
                && kosdaqRate.compareTo(MarketWeatherPolicy.INDEX_ONLY_CLEAR_KOSDAQ) >= 0) {
            return marketWeatherFactory.create(MarketWeatherLevel.CLEAR, MarketWeatherReason.INDEX_ONLY_RALLY, asOfDate);
        }
        if (kospiRate.compareTo(MarketWeatherPolicy.INDEX_ONLY_SUNNY_KOSPI) >= 0
                && kosdaqRate.compareTo(BigDecimal.ZERO) >= 0) {
            return marketWeatherFactory.create(MarketWeatherLevel.SUNNY, MarketWeatherReason.INDEX_ONLY_ADVANCE, asOfDate);
        }
        if (kospiRate.compareTo(MarketWeatherPolicy.INDEX_ONLY_PARTLY_CLOUDY_INDEX) >= 0
                || kosdaqRate.compareTo(MarketWeatherPolicy.INDEX_ONLY_PARTLY_CLOUDY_INDEX) >= 0) {
            return marketWeatherFactory.create(MarketWeatherLevel.PARTLY_CLOUDY, MarketWeatherReason.INDEX_ONLY_MIXED, asOfDate);
        }
        if (kospiRate.compareTo(MarketWeatherPolicy.INDEX_ONLY_STORMY_KOSPI) <= 0
                || kosdaqRate.compareTo(MarketWeatherPolicy.INDEX_ONLY_STORMY_KOSDAQ) <= 0) {
            return marketWeatherFactory.create(MarketWeatherLevel.STORMY, MarketWeatherReason.INDEX_ONLY_STORM, asOfDate);
        }
        if (kospiRate.compareTo(MarketWeatherPolicy.INDEX_ONLY_RAINY_KOSPI) <= 0
                || kosdaqRate.compareTo(MarketWeatherPolicy.INDEX_ONLY_RAINY_KOSDAQ) <= 0) {
            return marketWeatherFactory.create(MarketWeatherLevel.RAINY, MarketWeatherReason.INDEX_ONLY_WEAKNESS, asOfDate);
        }
        return marketWeatherFactory.create(MarketWeatherLevel.CLOUDY, MarketWeatherReason.INDEX_ONLY_SIDEWAYS, asOfDate);
    }
}
