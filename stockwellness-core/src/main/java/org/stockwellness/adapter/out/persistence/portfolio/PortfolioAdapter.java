package org.stockwellness.adapter.out.persistence.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.portfolio.LoadAdvisorPort;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.portfolio.SaveAdvisorPort;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.advisor.AdvisorReport;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PortfolioAdapter implements PortfolioPort, LoadAdvisorPort, SaveAdvisorPort {

    private final PortfolioRepository portfolioRepository;
    private final AdvisorReportRepository advisorReportRepository;

    @Override
    public Optional<Portfolio> findById(Long id) {
        return portfolioRepository.findById(id);
    }

    @Override
    public Optional<Portfolio> loadPortfolio(Long id, Long memberId) {
        return portfolioRepository.findWithItems(id, memberId);
    }

    @Override
    public List<Portfolio> loadAllPortfolios(Long memberId) {
        return portfolioRepository.findAllByMemberIdWithItems(memberId);
    }

    @Override
    public boolean existsPortfolioName(Long memberId, String name) {
        return portfolioRepository.existsByMemberIdAndName(memberId, name);
    }

    @Override
    public Portfolio savePortfolio(Portfolio portfolio) {
        return portfolioRepository.save(portfolio);
    }

    @Override
    public void deletePortfolio(Long portfolioId) {
        portfolioRepository.deleteById(portfolioId);
    }

    @Override
    public Optional<AdvisorReport> loadLatestReport(Long portfolioId) {
        return advisorReportRepository.findFirstByPortfolioIdOrderByCreatedAtDesc(portfolioId);
    }

    @Override
    public AdvisorReport saveReport(AdvisorReport report) {
        return advisorReportRepository.save(report);
    }
    }