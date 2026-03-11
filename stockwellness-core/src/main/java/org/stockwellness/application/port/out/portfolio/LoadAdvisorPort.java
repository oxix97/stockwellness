package org.stockwellness.application.port.out.portfolio;

import org.stockwellness.domain.portfolio.advisor.AdvisorReport;
import java.util.Optional;

/**
 * AI 어드바이저 보고서 조회 포트
 */
public interface LoadAdvisorPort {
    /**
     * 특정 포트폴리오의 가장 최근 어드바이저 보고서를 조회한다.
     */
    Optional<AdvisorReport> loadLatestReport(Long portfolioId);
}
