package org.stockwellness.domain.stock.insight;

public sealed interface WeatherState permits WeatherState.Clear, WeatherState.Sunny, WeatherState.PartlyCloudy, WeatherState.Cloudy, WeatherState.Foggy, WeatherState.Rainy, WeatherState.Storm {
    
    String getIconEmoji();
    String getDescription();
    String getStateName();
    
    record Clear() implements WeatherState {
        @Override public String getIconEmoji() { return "☀️"; }
        @Override public String getDescription() { return "강력한 상승 장세"; }
        @Override public String getStateName() { return "CLEAR"; }
    }
    
    record Sunny() implements WeatherState {
        @Override public String getIconEmoji() { return "🌤️"; }
        @Override public String getDescription() { return "완만한 상승 추세"; }
        @Override public String getStateName() { return "SUNNY"; }
    }
    
    record PartlyCloudy() implements WeatherState {
        @Override public String getIconEmoji() { return "🌥️"; }
        @Override public String getDescription() { return "상승 후 조심스러운 횡보"; }
        @Override public String getStateName() { return "PARTLY_CLOUDY"; }
    }
    
    record Cloudy() implements WeatherState {
        @Override public String getIconEmoji() { return "⛅"; }
        @Override public String getDescription() { return "방향성 탐색 중"; }
        @Override public String getStateName() { return "CLOUDY"; }
    }

    record Foggy() implements WeatherState {
        @Override public String getIconEmoji() { return "🌫️"; }
        @Override public String getDescription() { return "불확실성 증가 주의"; }
        @Override public String getStateName() { return "FOGGY"; }
    }
    
    record Rainy() implements WeatherState {
        @Override public String getIconEmoji() { return "🌧️"; }
        @Override public String getDescription() { return "단기 하락 전환"; }
        @Override public String getStateName() { return "RAINY"; }
    }
    
    record Storm() implements WeatherState {
        @Override public String getIconEmoji() { return "⛈️"; }
        @Override public String getDescription() { return "패닉 셀링 / 강력 하락"; }
        @Override public String getStateName() { return "STORMY"; }
    }

    static WeatherState fromScore(int score) {
        if (score >= 90) return new Clear();
        if (score >= 75) return new Sunny();
        if (score >= 60) return new PartlyCloudy();
        if (score >= 40) return new Cloudy();
        if (score >= 25) return new Foggy();
        if (score >= 10) return new Rainy();
        return new Storm();
    }
}
