package org.stockwellness.adapter.out.persistence.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.portfolio.LoadPortfolioPort;
import org.stockwellness.application.port.out.portfolio.SavePortfolioPort;
import org.stockwellness.domain.portfolio.Portfolio;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PortfolioAdapter implements LoadPortfolioPort, SavePortfolioPort {

    private final PortfolioRepository portfolioRepository;

    @Override
    public Optional<Portfolio> loadPortfolio(Long id, Long memberId) {
        return portfolioRepository.findWithItems(id, memberId);
    }

    @Override
    public List<Portfolio> loadAllPortfolios(Long memberId) {
        return portfolioRepository.findAllByMemberId(memberId);
    }

    @Override
    public boolean existsPortfolioName(Long memberId, String name) {
        return portfolioRepository.existsByMemberIdAndName(memberId, name);
    }

    @Override
    public Portfolio savePortfolio(Portfolio portfolio) {
        return portfolioRepository.save(portfolio);
    }
}
