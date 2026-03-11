package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.application.port.out.stock.LoadTechnicalDataPort;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.analysis.MarketCondition;
import org.stockwellness.domain.stock.analysis.TechnicalCalculator;
import org.stockwellness.domain.stock.exception.StockDataNotFoundException;
import org.stockwellness.domain.stock.price.StockPrice;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockTechnicalDataAdapter implements LoadTechnicalDataPort {

    private static final int RECENT_PRICE_LIMIT = 2;
    private final StockPriceRepository stockPriceRepository;

    @Override
    public AiAnalysisContext loadTechnicalContext(String ticker) {
        List<StockPrice> histories = stockPriceRepository.findRecentPrices(ticker, LocalDate.now(), PageRequest.of(0, RECENT_PRICE_LIMIT));

        if (histories.isEmpty()) {
            throw new StockDataNotFoundException(ticker);
        }

        StockPrice today = histories.get(0);
        StockPrice yesterday = histories.size() > 1 ? histories.get(1) : null;

        MarketCondition condition = TechnicalCalculator.analyze(today, yesterday);

        return AiAnalysisContext.of(today, condition);
    }

    @Override
    public Map<String, AiAnalysisContext> loadTechnicalContexts(List<String> tickers) {
        List<StockPrice> allPrices = stockPriceRepository.findRecentPricesByTickers(tickers, LocalDate.now());

        Map<String, List<StockPrice>> historiesMap = allPrices.stream()
                .collect(Collectors.groupingBy(sp -> sp.getStock().getTicker()));

        return historiesMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            List<StockPrice> histories = e.getValue().stream()
                                    .sorted(Comparator.comparing((StockPrice sp) -> sp.getId().getBaseDate()).reversed())
                                    .limit(RECENT_PRICE_LIMIT)
                                    .toList();
                            
                            StockPrice today = histories.get(0);
                            StockPrice yesterday = histories.size() > 1 ? histories.get(1) : null;
                            MarketCondition condition = TechnicalCalculator.analyze(today, yesterday);
                            return AiAnalysisContext.of(today, condition);
                        }
                ));
    }
}
