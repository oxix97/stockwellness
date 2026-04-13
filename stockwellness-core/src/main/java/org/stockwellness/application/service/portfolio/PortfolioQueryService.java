package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.portfolio.LoadPortfolioUseCase;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioResponse;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.exception.PortfolioAccessDeniedException;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.domain.stock.Stock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioQueryService implements LoadPortfolioUseCase {

    private final PortfolioPort portfolioPort;
    private final StockPort stockPort;
    private final StockPricePort stockPricePort;

    @Override
    public PortfolioResponse getPortfolio(Long memberId, Long portfolioId) {
        Portfolio portfolio = loadOwnedPortfolio(portfolioId, memberId);
        Map<String, BigDecimal> latestPrices = getLatestPrices(portfolio);
        Map<String, Stock> stockMap = getStockMap(portfolio.getItems().stream()
                .map(PortfolioItem::getSymbol)
                .distinct()
                .toList());
        return PortfolioResponse.from(portfolio, latestPrices, stockMap);
    }

    @Override
    public List<PortfolioResponse> getMyPortfolios(Long memberId) {
        List<Portfolio> portfolios = portfolioPort.loadAllPortfolios(memberId);
        
        // [N+1 해결] 모든 포트폴리오의 종목 티커를 한 번에 수집하여 최신 시세 일괄 조회
        List<String> allTickers = portfolios.stream()
                .flatMap(p -> p.getItems().stream())
                .map(PortfolioItem::getSymbol)
                .distinct()
                .toList();
        
        Map<String, BigDecimal> latestPriceMap = stockPricePort.findAllLatestByTickers(allTickers);
        Map<String, Stock> stockMap = getStockMap(allTickers);

        return portfolios.stream()
                .map(p -> {
                    // 각 포트폴리오에 필요한 티커들만 추출하여 맵 생성
                    Map<String, BigDecimal> latestPrices = p.getItems().stream()
                            .map(PortfolioItem::getSymbol)
                            .distinct()
                            .collect(Collectors.toMap(
                                    symbol -> symbol,
                                    symbol -> latestPriceMap.getOrDefault(symbol, BigDecimal.ZERO)
                            ));
                    return PortfolioResponse.from(p, latestPrices, stockMap);
                })
                .toList();
    }

    private Map<String, BigDecimal> getLatestPrices(Portfolio portfolio) {
        List<String> tickers = portfolio.getItems().stream()
                .map(PortfolioItem::getSymbol)
                .distinct()
                .toList();
        return stockPricePort.findAllLatestByTickers(tickers);
    }

    private Map<String, Stock> getStockMap(List<String> tickers) {
        return stockPort.loadStocksByTickers(tickers).stream()
                .collect(Collectors.toMap(Stock::getTicker, stock -> stock));
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
