package org.stockwellness.adapter.out.persistence.portfolio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.portfolio.advisor.AdvisorReport;

public interface AdvisorReportRepository extends JpaRepository<AdvisorReport, Long> {
    Optional<AdvisorReport> findFirstByPortfolioIdOrderByGeneratedAtDescIdDesc(Long portfolioId);
}
