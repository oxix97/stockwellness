package org.stockwellness.application.port.out.portfolio;

import org.stockwellness.domain.portfolio.advisor.AdvisorReport;

/**
 * AI 어드바이저 보고서 저장 포트
 */
public interface SaveAdvisorPort {
    /**
     * 어드바이저 보고서를 저장한다.
     */
    AdvisorReport saveReport(AdvisorReport report);
}
