package org.stockwellness.domain.stock.analysis;

import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AiAnalysisContext(
        String isinCode,            // 종목 코드
        LocalDate baseDate,         // 데이터 기준일
        PriceSummary priceInfo,     // 가격 정보
        TechnicalSignal technicalSignal, // 기술적 분석 지표 (핵심)
        PortfolioRisk riskInfo      // 리스크 정보 (변동성 등)
) {

    public static AiAnalysisContext of(StockPrice today, MarketCondition condition) {
        return new AiAnalysisContext(
                today.getStock().getTicker(), // Ticker를 종목 식별자로 사용
                today.getId().getBaseDate(),
                PriceSummary.from(today),
                TechnicalSignal.of(today, condition),
                new PortfolioRisk(false, 0.0)
        );
    }

    // 1. 가격 정보 그룹
    public record PriceSummary(
            BigDecimal closePrice,      // 종가
            BigDecimal fluctuationRate, // 등락률 (전일 대비)
            BigDecimal volume           // 거래량 (보조 지표용)
    ) {
        public static PriceSummary from(StockPrice price) {
            return new PriceSummary(
                    price.getClosePrice(),
                    price.getFluctuationRate(),
                    BigDecimal.valueOf(price.getVolume() != null ? price.getVolume() : 0)
            );
        }
    }

    // 2. 기술적 신호 그룹 (도메인 Enum 활용)
    public record TechnicalSignal(
            TrendStatus trendStatus,    // 추세 (정배열, 역배열)
            BigDecimal rsi,             // RSI 수치
            String rsiStatus,           // RSI 해석 (예: "과매수 구간", "중립")
            BigDecimal macd,            // MACD 수치
            CrossoverSignal signal,     // 골든크로스/데드크로스 여부

            // 이동평균선 수치 (AI가 구체적 수치를 참고할 수 있게 제공)
            BigDecimal ma5,
            BigDecimal ma20,
            BigDecimal ma60,
            BigDecimal ma120
    ) {
        public static TechnicalSignal of(StockPrice price, MarketCondition condition) {
            TechnicalIndicators ind = price.getIndicators();
            BigDecimal rsi = ind != null ? ind.getRsi14() : null;
            
            return new TechnicalSignal(
                    condition.trendStatus(),
                    rsi,
                    TechnicalCalculator.analyzeRsiLevel(rsi),
                    ind != null ? ind.getMacd() : null,
                    condition.signal(),
                    ind != null ? ind.getMa5() : null,
                    ind != null ? ind.getMa20() : null,
                    ind != null ? ind.getMa60() : null,
                    ind != null ? ind.getMa120() : null
            );
        }
    }

    // 3. 리스크 정보 그룹 (확장성 고려)
    public record PortfolioRisk(
            boolean isHighRisk,         // 관리종목 여부 등
            double volatilityScore      // 변동성 점수 (0.0 ~ 100.0)
    ) {}
}
