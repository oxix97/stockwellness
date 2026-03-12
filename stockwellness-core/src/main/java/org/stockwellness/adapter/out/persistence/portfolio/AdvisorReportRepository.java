package org.stockwellness.adapter.out.persistence.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.portfolio.advisor.AdvisorReport;

import java.util.Optional;

public interface AdvisorReportRepository extends JpaRepository<AdvisorReport, Long> {
    /**
     * 특정 포트폴리오 ID에 해당하는 가장 최근 보고서를 조회한다.
     * AbstractEntity의 createdAt을 기준으로 내림차순 정렬하여 첫 번째 결과를 가져온다.
     */
    Optional<AdvisorReport> findFirstByPortfolioIdOrderByCreatedAtDesc(Long portfolioId);
}
