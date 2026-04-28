package org.stockwellness.domain.stock.insight;

public sealed interface WeatherState permits WeatherState.Sunny, WeatherState.PartlyCloudy, WeatherState.Cloudy, WeatherState.Rainy, WeatherState.Storm {
    
    String getIconEmoji();
    String getDescription();
    
    record Sunny() implements WeatherState {
        @Override public String getIconEmoji() { return "☀️"; }
        @Override public String getDescription() { return "안정적인 상승 추세"; }
    }
    
    record PartlyCloudy() implements WeatherState {
        @Override public String getIconEmoji() { return "⛅"; }
        @Override public String getDescription() { return "상승 후 조심스러운 장세"; }
    }
    
    record Cloudy() implements WeatherState {
        @Override public String getIconEmoji() { return "☁️"; }
        @Override public String getDescription() { return "방향성 탐색 구간"; }
    }
    
    record Rainy() implements WeatherState {
        @Override public String getIconEmoji() { return "🌧️"; }
        @Override public String getDescription() { return "하락 전환 주의"; }
    }
    
    record Storm() implements WeatherState {
        @Override public String getIconEmoji() { return "⛈️"; }
        @Override public String getDescription() { return "패닉 셀링 / 강한 하락"; }
    }

    static WeatherState fromScore(int score) {
        if (score >= 80) return new Sunny();
        if (score >= 60) return new PartlyCloudy();
        if (score >= 40) return new Cloudy();
        if (score >= 20) return new Rainy();
        return new Storm();
    }
}
