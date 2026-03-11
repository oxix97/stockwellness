package org.stockwellness.application.port.out.portfolio;

import org.stockwellness.domain.portfolio.advisor.AdviceAction;

/**
 * AI 어드바이저 분석 수행 포트
 */
public interface AiAdvisorPort {
    /**
     * 포트폴리오 데이터를 기반으로 AI 리밸런싱 조언을 생성한다.
     */
    AdvisorAiResult getRebalancingAdvice(AdvisorAiContext context);

    record AdvisorAiResult(
            String targetAlignment,
            String technicalInsight,
            String riskStrategy,
            String adviceContent,
            AdviceAction primaryAction
    ) {
        public static AdvisorAiResult fallback() {
            return new AdvisorAiResult(
                    "데이터 분석 중 오류가 발생했습니다.",
                    "기술적 지표를 불러올 수 없습니다.",
                    "현재 시장 상황을 분석할 수 없습니다.",
                    "AI 서버와의 통신에 실패하여 리밸런싱 조언을 생성하지 못했습니다. 잠시 후 다시 시도해주세요.",
                    AdviceAction.REBALANCE
            );
        }
    }
}
