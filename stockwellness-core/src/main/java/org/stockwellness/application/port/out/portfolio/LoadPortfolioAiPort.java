package org.stockwellness.application.port.out.portfolio;

import org.stockwellness.application.port.in.portfolio.result.PortfolioAiResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioHealthResult;

public interface LoadPortfolioAiPort {
    /**
     * AI에게 포트폴리오 건강 진단 결과를 바탕으로 인사이트 생성을 요청합니다.
     * @param context AI 분석을 위한 데이터 컨텍스트
     * @return AI가 생성한 요약, 통찰 및 다음 단계 제안
     */
    PortfolioAiResult generatePortfolioInsight(PortfolioAiContext context);
}
