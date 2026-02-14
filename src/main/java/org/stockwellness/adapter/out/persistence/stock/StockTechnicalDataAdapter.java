package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.application.port.out.stock.LoadTechnicalDataPort;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockTechnicalDataAdapter implements LoadTechnicalDataPort {

    private static final int RECENT_stockPrice_LIMIT = 2;
    private final StockPriceRepository stockPriceRepository;

    @Override
    public AiAnalysisContext loadTechnicalContext(String isinCode) {
        return null;
//        List<StockPrice> histories = stockPriceRepository.findRecentstockPrice(isinCode, LocalDate.now(), RECENT_stockPrice_LIMIT);

//        if (histories.isEmpty()) {
//            throw new StockDataNotFoundException(isinCode);
//        }

//        StockPrice today = histories.get(0);
//        StockPrice yesterday = histories.size() > 1 ? histories.get(1) : null;

//        MarketCondition condition = TechnicalCalculator.analyze(today, yesterday);

//        return AiAnalysisContext.of(today, condition);
    }

    @Override
    public Map<String, AiAnalysisContext> loadTechnicalContexts(List<String> isinCodes) {
        return null;
//        Map<String, List<StockPrice>> historiesMap = stockPriceRepository.findRecentstockPriceBatch(isinCodes, RECENT_stockPrice_LIMIT);
//
//        return historiesMap.entrySet().stream()
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        e -> {
//                            List<StockPrice> histories = e.getValue();
//                            StockPrice today = histories.get(0);
//                            StockPrice yesterday = histories.size() > 1 ? histories.get(1) : null;
//                            MarketCondition condition = TechnicalCalculator.analyze(today, yesterday);
//                            return AiAnalysisContext.of(today, condition);
//                        }
//                ));
    }
}