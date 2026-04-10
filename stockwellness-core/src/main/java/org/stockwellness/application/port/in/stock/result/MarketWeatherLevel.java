package org.stockwellness.application.port.in.stock.result;

public enum MarketWeatherLevel {
    CLEAR("오늘의 증시는 쾌청해요", "시장 전반에 온기가 퍼지고 있어요", "positive"),
    SUNNY("오늘의 증시는 맑음이에요", "투자심리가 비교적 안정적인 편이에요", "positive"),
    PARTLY_CLOUDY("오늘의 증시는 구름 조금이에요", "종목별 분위기를 조금 더 지켜볼 필요가 있어요", "mixed-positive"),
    CLOUDY("오늘의 증시는 흐림이에요", "뚜렷한 방향성이 보이지 않아요", "neutral"),
    FOGGY("오늘의 증시는 안개가 꼈어요", "지수와 체감 사이에 온도차가 있어요", "mixed-negative"),
    RAINY("오늘의 증시는 비가 내려요", "시장 전반에 매도 압력이 퍼지고 있어요", "negative"),
    STORMY("오늘의 증시는 폭우예요", "변동성이 커져 방어적으로 볼 필요가 있어요", "negative");

    private final String headline;
    private final String defaultDescription;
    private final String toneKey;

    MarketWeatherLevel(String headline, String defaultDescription, String toneKey) {
        this.headline = headline;
        this.defaultDescription = defaultDescription;
        this.toneKey = toneKey;
    }

    public String headline() {
        return headline;
    }

    public String defaultDescription() {
        return defaultDescription;
    }

    public String toneKey() {
        return toneKey;
    }
}
