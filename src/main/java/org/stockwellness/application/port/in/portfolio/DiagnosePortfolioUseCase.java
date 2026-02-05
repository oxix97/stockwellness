package org.stockwellness.application.port.in.portfolio;

import org.stockwellness.application.port.in.portfolio.result.PortfolioHealthResult;

public interface DiagnosePortfolioUseCase {
    /**
     * 포트폴리오 건강 상태를 진단하고 AI 인사이트를 생성합니다.
     * @param memberId 회원 ID (소유권 확인용)
     * @param portfolioId 포트폴리오 ID
     * @return 진단 결과 및 AI 조언
     */
    PortfolioHealthResult diagnosePortfolio(Long memberId, Long portfolioId);
}
