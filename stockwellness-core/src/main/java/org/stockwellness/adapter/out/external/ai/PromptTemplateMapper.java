package org.stockwellness.adapter.out.external.ai;

import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.portfolio.PortfolioAiContext;
import org.stockwellness.application.port.out.sector.SectorAiContext;
import org.stockwellness.domain.portfolio.diagnosis.type.DiagnosisCategory;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.analysis.CrossoverSignal;
import org.stockwellness.domain.stock.analysis.TrendStatus;
import org.stockwellness.global.util.FinanceFormatUtil;

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

    private static final String SECTOR_PROMPT_TEMPLATE = """
        [분석 대상 섹터 데이터]
        - 섹터명: %s (%s, %s 시장)
        - 기준일자: %s
        - 현재 지수: %s (평균 등락률: %s%%)
        
        [수급 및 추세 상황]
        - 외국인 순매수액: %s (연속 %d일 매수)
        - 기관 순매수액: %s (연속 %d일 매수)
        - 추세 상태: %s
        - 섹터 RSI: %s (%s)
        
        [섹터 내 핵심 주도주 흐름]
        %s
        
        위 섹터의 현재 체력과 수급 상황을 분석하여, 투자 전략(매수/매도/관망)과 그 근거를 제시해줘.
        """;

    private static final String SECTOR_SYSTEM_INSTRUCTION = """
            당신은 "여의도 1타 애널리스트" 스타일의 시장 분석 전문가입니다. 
            주어진 섹터의 지수 흐름, 수급 상황, 주도주들의 움직임을 종합하여 명쾌한 투자 의견을 제시합니다.
            
            [분석 가이드]
            1. decision: 해당 섹터에 대한 최종 판단 (BUY, SELL, HOLD)
            2. title: 시장의 눈길을 끄는 강렬한 섹터 헤드라인 (15자 이내)
            3. keyReasons: 분석 결과 도출된 핵심 근거 3가지 (각 항목은 20자 이내)
            4. detailedAnalysis: 수급(외인/기관), 추세(이평선), 주도주 상황을 구체적으로 설명하는 분석문
            
            전문적이면서도 투자자가 실질적인 행동 지침을 얻을 수 있도록 단호하게 조언하세요.
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

    private static final String PORTFOLIO_SYSTEM_INSTRUCTION = """
            당신은 "성장하는 궁수(Growth Archer)" 스타일의 투자 전문가입니다. 
            초보 투자자들에게 친근하고 이해하기 쉬운 언어로 포트폴리오 상태를 진단해 줍니다.
            
            [응답 지침]
            1. summary: 포트폴리오의 특징을 나타내는 짧은 별명 (예: "안정적인 방패", "공격적인 불화살")
            2. insight: 현재 포트폴리오의 강점과 약점을 분석한 총평 (2~3문장)
            3. nextSteps: 포트폴리오 개선을 위한 구체적인 실행 단계 (리스트 형태, 최소 2개)
            
            반드시 초보자의 눈높이에 맞춰 친절하게 설명하세요.
            """;

    public String getPortfolioSystemInstruction() {
        return PORTFOLIO_SYSTEM_INSTRUCTION;
    }

    public String getSectorSystemInstruction() {
        return SECTOR_SYSTEM_INSTRUCTION;
    }

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

    public String toSectorPromptString(SectorAiContext context) {
        String leadingStocksInfo = context.leadingStocks().stream()
                .map(s -> String.format("   - %s (%s%%, 수급: %s)", s.name(), 
                        FinanceFormatUtil.formatRate(s.fluctuationRate()), 
                        FinanceFormatUtil.formatAmount(s.transactionAmt())))
                .collect(Collectors.joining("\n"));

        return SECTOR_PROMPT_TEMPLATE.formatted(
                context.sectorName(),
                context.sectorCode(),
                context.marketType().getDescription(),
                context.baseDate(),
                FinanceFormatUtil.formatDecimal(context.indexPrice()),
                FinanceFormatUtil.formatRate(context.fluctuationRate()),
                FinanceFormatUtil.formatAmount(context.netForeignBuy()),
                context.foreignDays(),
                FinanceFormatUtil.formatAmount(context.netInstBuy()),
                context.instDays(),
                translateTrend(context.trendStatus()),
                FinanceFormatUtil.formatDecimal(context.rsi()),
                context.isOverheated() ? "과열" : "정상",
                leadingStocksInfo
        );
    }

    public String toPromptString(AiAnalysisContext context) {
        var priceInfo = context.priceInfo();
        var techInfo = context.technicalSignal();

        return USER_PROMPT_TEMPLATE.formatted(
                // 1. 기본 정보
                context.isinCode(),
                context.baseDate(),
                FinanceFormatUtil.formatPrice(priceInfo.closePrice()),
                FinanceFormatUtil.formatRate(priceInfo.fluctuationRate()),

                // 2. 추세 및 이평선 (Enum -> Korean)
                translateTrend(techInfo.trendStatus()),
                FinanceFormatUtil.formatPrice(techInfo.ma5()),
                FinanceFormatUtil.formatPrice(techInfo.ma20()),
                FinanceFormatUtil.formatPrice(techInfo.ma60()),
                FinanceFormatUtil.formatPrice(techInfo.ma120()),

                // 3. 모멘텀
                FinanceFormatUtil.formatDecimal(techInfo.rsi()), // RSI 수치
                techInfo.rsiStatus(),          // "과매수" 등 텍스트
                translateSignal(techInfo.signal()), // 골든크로스 여부
                FinanceFormatUtil.formatDecimal(techInfo.macd())
        );
    }

    // --- Helper Methods (Translation) ---

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
}
