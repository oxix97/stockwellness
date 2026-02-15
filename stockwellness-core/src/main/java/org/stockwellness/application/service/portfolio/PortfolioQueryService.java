package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioResponse;
import org.stockwellness.application.port.in.portfolio.LoadPortfolioUseCase;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.exception.PortfolioAccessDeniedException;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioQueryService implements LoadPortfolioUseCase {

    private final PortfolioPort portfolioPort;

    @Override
    public PortfolioResponse getPortfolio(Long memberId, Long portfolioId) {
        Portfolio portfolio = loadOwnedPortfolio(portfolioId, memberId);
        return PortfolioResponse.from(portfolio);
    }

    @Override
    public List<PortfolioResponse> getMyPortfolios(Long memberId) {
        return portfolioPort.loadAllPortfolios(memberId).stream()
                .map(PortfolioResponse::from)
                .toList();
    }

    private Portfolio loadOwnedPortfolio(Long portfolioId, Long memberId) {
        return portfolioPort.loadPortfolio(portfolioId, memberId)
                .orElseThrow(() -> {
                    if (portfolioPort.findById(portfolioId).isPresent()) {
                        throw new PortfolioAccessDeniedException();
                    }
                    return new PortfolioNotFoundException();
                });
    }
}