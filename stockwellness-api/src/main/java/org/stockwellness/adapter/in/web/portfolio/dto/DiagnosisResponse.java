package org.stockwellness.adapter.in.web.portfolio.dto;

import org.stockwellness.application.port.in.portfolio.result.PortfolioHealthResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 포트폴리오 진단 결과 응답 DTO
 */
public record DiagnosisResponse(
        /**
         * 포트폴리오 종합 건강 점수 (0~100)
         */
        int overallScore,
        
        /**
         * 5개 카테고리별 상세 점수
         */
        Map<String, Integer> categories,
        
        /**
         * 종목별 포트폴리오 기여도 분석 결과
         */
        List<StockContributionResponse> stockContributions,

        /**
         * 최대 낙폭 (MDD)
         */
        BigDecimal mdd,

        /**
         * 벤치마크 대비 상대 낙폭
         */
        BigDecimal relativeMdd,

        /**
         * 샤프 지수
         */
        BigDecimal sharpeRatio,

        /**
         * 초과 수익률 (Alpha)
         */
        BigDecimal alpha,
        
        /**
         * AI가 생성한 진단 요약 한 줄
         */
        String summary,
        
        /**
         * 상세 진단 인사이트 (Markdown 형식)
         */
        String insight,
        
        /**
         * 개선을 위한 향후 조언 단계별 리스트
         */
        List<String> nextSteps
) {
    public static DiagnosisResponse from(PortfolioHealthResult health) {
        return new DiagnosisResponse(
                health.overallScore(),
                health.categories(),
                health.stockContributions().stream()
                        .map(StockContributionResponse::from)
                        .toList(),
                health.mdd(),
                health.relativeMdd(),
                health.sharpeRatio(),
                health.alpha(),
                health.summary(),
                health.insight(),
                health.nextSteps()
        );
    }
}
