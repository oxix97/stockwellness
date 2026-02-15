package org.stockwellness.domain.stock.analysis;

import org.stockwellness.domain.stock.StockPrice;

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
        return null;
//        return new AiAnalysisContext(
//                today.getIsinCode(),
//                today.getBaseDate(),
//                PriceSummary.from(today),
//                TechnicalSignal.of(today, condition),
//                new PortfolioRisk(false, 0.0)
//        );
    }

    // 1. 가격 정보 그룹
    public record PriceSummary(
            BigDecimal closePrice,      // 종가
            BigDecimal fluctuationRate, // 등락률 (전일 대비)
            BigDecimal volume           // 거래량 (보조 지표용)
    ) {
        public static PriceSummary from(StockPrice price) {
            return null;
//            return new PriceSummary(
//                    price.getClosePrice(),
//                    price.getFluctuationRate(),
//                    new BigDecimal(price.getVolume())
//            );
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
            return null;
//            return new TechnicalSignal(
//                    condition.trendStatus(),
//                    price.getRsi14(),
//                    TechnicalCalculator.analyzeRsiLevel(price.getRsi14()),
//                    price.getMacd(),
//                    condition.signal(),
//                    price.getMa5(),
//                    price.getMa20(),
//                    price.getMa60(),
//                    price.getMa120()
//            );
        }
    }

    // 3. 리스크 정보 그룹 (확장성 고려)
    public record PortfolioRisk(
            boolean isHighRisk,         // 관리종목 여부 등
            double volatilityScore      // 변동성 점수 (0.0 ~ 100.0)
    ) {}
}