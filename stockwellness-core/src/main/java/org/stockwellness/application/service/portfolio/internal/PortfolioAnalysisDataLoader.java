package org.stockwellness.application.service.portfolio.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioStatsRepository;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.PortfolioStats;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.KisMultiStockPriceDetail;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PortfolioAnalysisDataLoader {

    private final PortfolioPort portfolioPort;
    private final StockPort stockPort;
    private final StockPricePort stockPricePort;
    private final PortfolioStatsRepository portfolioStatsRepository;
    private final KisDailyPriceAdapter kisDailyPriceAdapter;

    public AnalysisContext loadContext(Long portfolioId, Long memberId) {
        Portfolio portfolio = portfolioPort.loadPortfolio(portfolioId, memberId)
                .orElseThrow(PortfolioNotFoundException::new);

        List<String> symbols = portfolio.getItems().stream()
                .filter(item -> item.getAssetType() == org.stockwellness.domain.portfolio.AssetType.STOCK)
                .map(PortfolioItem::getSymbol)
                .toList();

        Map<String, Stock> stockMap = stockPort.loadStocksByTickers(symbols).stream()
                .collect(Collectors.toMap(Stock::getTicker, s -> s));

        // 1. DB에서 최신 시세 조회
        Map<String, List<StockPrice>> priceMap = new HashMap<>(stockPricePort.loadRecentHistoriesBatch(symbols, 1));

        // 2. [Fallback] DB에 데이터가 없는 종목들은 KIS API로 실시간 시세 조회
        List<String> missingSymbols = symbols.stream()
                .filter(s -> !priceMap.containsKey(s) || priceMap.get(s).isEmpty())
                .toList();

        if (!missingSymbols.isEmpty()) {
            fetchAndFillMissingPrices(missingSymbols, priceMap, stockMap);
        }

        PortfolioStats stats = portfolioStatsRepository.findByPortfolioId(portfolioId).orElse(null);

        return new AnalysisContext(portfolio, stockMap, priceMap, stats);
    }

    private void fetchAndFillMissingPrices(List<String> symbols, Map<String, List<StockPrice>> priceMap, Map<String, Stock> stockMap) {
        // 최대 30개씩 끊어서 멀티 조회
        for (int i = 0; i < symbols.size(); i += 30) {
            List<String> chunk = symbols.subList(i, Math.min(i + 30, symbols.size()));
            List<KisMultiStockPriceDetail> details = kisDailyPriceAdapter.fetchMultiStockPrices(chunk);
            
            for (KisMultiStockPriceDetail detail : details) {
                Stock stock = stockMap.get(detail.ticker());
                if (stock != null) {
                    BigDecimal price = new BigDecimal(detail.closePrice());
                    // 임시 StockPrice 객체 생성 (계산용으로만 사용)
                    StockPrice tempPrice = StockPrice.of(stock, java.time.LocalDate.now(), 
                            new BigDecimal(detail.openPrice()), new BigDecimal(detail.highPrice()), 
                            new BigDecimal(detail.lowPrice()), price, price, 
                            new BigDecimal(detail.previousClosePrice()), 
                            Long.parseLong(detail.accumulatedVolume()), 
                            new BigDecimal(detail.accumulatedTradingValue()),
                            BigDecimal.ZERO, BigDecimal.ZERO, null);
                    
                    priceMap.put(detail.ticker(), List.of(tempPrice));
                }
            }
        }
    }
}
