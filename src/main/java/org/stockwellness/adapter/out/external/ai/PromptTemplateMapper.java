package org.stockwellness.adapter.out.external.ai;

import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.portfolio.result.PortfolioHealthResult;
import org.stockwellness.application.port.out.portfolio.PortfolioAiContext;
import org.stockwellness.domain.portfolio.diagnosis.type.DiagnosisCategory;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.analysis.CrossoverSignal;
import org.stockwellness.domain.stock.analysis.TrendStatus;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.stream.Collectors;

@Component
public class PromptTemplateMapper {

    // AI에게 제공할 데이터 템플릿 (Structured Text)
    private static final String USER_PROMPT_TEMPLATE = """
        [분석 대상 데이터]
        - 종목코드: %s
        - 기준일자: %s
        - 현재주가: %s (전일대비 %s%%)
        
        [기술적 지표 상세]
        1. 추세 상태: %s
        2. 이동평균선 수치:
           - 5일선: %s
           - 20일선: %s
           - 60일선: %s
           - 120일선: %s
        3. 모멘텀 지표:
           - RSI(14): %s (%s)
           - MACD 신호: %s (MACD: %s)
        
        위 데이터를 바탕으로 매수/매도 심리를 분석하고 향후 방향성을 예측해줘.
        """;

    private static final String PORTFOLIO_PROMPT_TEMPLATE = """
        [포트폴리오 건강 진단 데이터]
        - 종합 점수: %d / 100
        
        [카테고리별 점수]
        - 방어력 (Defense): %d
        - 공격력 (Attack): %d
        - 지구력 (Endurance): %d
        - 민첩성 (Agility): %d
        - 균형성 (Balance): %d
        
        위 점수들을 바탕으로 현재 포트폴리오의 상태를 진단하고, 초보 투자자가 이해하기 쉬운 언어로 조언을 해줘.
        """;

    public String toPortfolioPromptString(PortfolioAiContext context) {
        var categories = context.categories();
        return PORTFOLIO_PROMPT_TEMPLATE.formatted(
                context.overallScore(),
                categories.getOrDefault(DiagnosisCategory.DEFENSE.getKey(), 0),
                categories.getOrDefault(DiagnosisCategory.ATTACK.getKey(), 0),
                categories.getOrDefault(DiagnosisCategory.ENDURANCE.getKey(), 0),
                categories.getOrDefault(DiagnosisCategory.AGILITY.getKey(), 0),
                categories.getOrDefault(DiagnosisCategory.BALANCE.getKey(), 0)
        );
    }

    public String toPromptString(AiAnalysisContext context) {
        var priceInfo = context.priceInfo();
        var techInfo = context.technicalSignal();

        return USER_PROMPT_TEMPLATE.formatted(
                // 1. 기본 정보
                context.isinCode(),
                context.baseDate(),
                formatPrice(priceInfo.closePrice()),
                formatRate(priceInfo.fluctuationRate()),

                // 2. 추세 및 이평선 (Enum -> Korean)
                translateTrend(techInfo.trendStatus()),
                formatPrice(techInfo.ma5()),
                formatPrice(techInfo.ma20()),
                formatPrice(techInfo.ma60()),
                formatPrice(techInfo.ma120()),

                // 3. 모멘텀
                formatDecimal(techInfo.rsi()), // RSI 수치
                techInfo.rsiStatus(),          // "과매수" 등 텍스트
                translateSignal(techInfo.signal()), // 골든크로스 여부
                formatDecimal(techInfo.macd())
        );
    }

    // --- Helper Methods (Formatting & Translation) ---

    private String translateTrend(TrendStatus status) {
        if (status == null) return "판단 불가";
        return switch (status) {
            case REGULAR -> "정배열 (강한 상승 추세)";
            case INVERSE -> "역배열 (강한 하락 추세)";
            case NEUTRAL -> "혼조세 (박스권 또는 방향 탐색 중)";
            default -> "중립";
        };
    }

    private String translateSignal(CrossoverSignal signal) {
        if (signal == null) return "특이사항 없음";
        return switch (signal) {
            case GOLDEN_CROSS -> "★ 골든크로스 발생 (단기 매수 신호)";
            case DEAD_CROSS -> "⚠️ 데드크로스 발생 (단기 매도 신호)";
            case NONE -> "크로스 신호 없음";
        };
    }

    // 통화 포맷 (천단위 콤마)
    private String formatPrice(BigDecimal price) {
        if (price == null) return "0";
        // 1000원 미만(동전주)이거나 달러일 경우 소수점 표시, 그 외는 정수
        if (price.compareTo(new BigDecimal("1000")) < 0) {
            return new DecimalFormat("#,##0.00").format(price);
        }
        return new DecimalFormat("#,###").format(price);
    }

    // 등락률, RSI 등 소수점 포맷
    private String formatRate(BigDecimal rate) {
        if (rate == null) return "0.00";
        return new DecimalFormat("+#,##0.00;-#").format(rate); // 양수엔 + 붙임
    }

    // 일반 지표 포맷
    private String formatDecimal(BigDecimal value) {
        if (value == null) return "0.00";
        return new DecimalFormat("#,##0.00").format(value);
    }
}
