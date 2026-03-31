package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioResponse;
import org.stockwellness.application.port.in.portfolio.LoadPortfolioUseCase;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.exception.PortfolioAccessDeniedException;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioQueryService implements LoadPortfolioUseCase {

    private final PortfolioPort portfolioPort;
    private final StockPricePort stockPricePort;

    @Override
    public PortfolioResponse getPortfolio(Long memberId, Long portfolioId) {
        Portfolio portfolio = loadOwnedPortfolio(portfolioId, memberId);
        Map<String, BigDecimal> latestPrices = getLatestPrices(portfolio);
        return PortfolioResponse.from(portfolio, latestPrices);
    }

    @Override
    public List<PortfolioResponse> getMyPortfolios(Long memberId) {
        List<Portfolio> portfolios = portfolioPort.loadAllPortfolios(memberId);
        return portfolios.stream()
                .map(p -> {
                    Map<String, BigDecimal> latestPrices = getLatestPrices(p);
                    return PortfolioResponse.from(p, latestPrices);
                })
                .toList();
    }

    private Map<String, BigDecimal> getLatestPrices(Portfolio portfolio) {
        return portfolio.getItems().stream()
                .map(PortfolioItem::getSymbol)
                .distinct()
                .collect(Collectors.toMap(
                        symbol -> symbol,
                        symbol -> stockPricePort.findLatestByTicker(symbol)
                                .map(StockPrice::getClosePrice)
                                .orElse(BigDecimal.ZERO)
                ));
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