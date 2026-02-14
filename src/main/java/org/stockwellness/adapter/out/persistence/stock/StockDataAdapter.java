package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.LoadStockPricePort;
import org.stockwellness.application.port.out.stock.LoadTechnicalDataPort;
import org.stockwellness.application.port.out.watchlist.StockDataPort;
import org.stockwellness.domain.stock.StockPrice;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.analysis.TechnicalCalculator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StockDataAdapter implements StockDataPort {

    private final LoadStockPricePort loadStockPricePort;
    private final LoadTechnicalDataPort loadTechnicalDataPort;

    @Override
    public Map<String, StockWellnessDetail> getStockDetails(List<String> isinCodes) {
        Map<String, List<StockPrice>> latestHistories = loadStockPricePort.loadRecentHistoriesBatch(isinCodes, 1);
        Map<String, AiAnalysisContext> technicalContexts = loadTechnicalDataPort.loadTechnicalContexts(isinCodes);

        return isinCodes.stream()
                .filter(isinCode -> latestHistories.containsKey(isinCode) && !latestHistories.get(isinCode).isEmpty())
                .collect(Collectors.toMap(
                        isinCode -> isinCode,
                        isinCode -> convertToDetail(
                                latestHistories.get(isinCode).get(0),
                                technicalContexts.get(isinCode)
                        )
                ));
    }

    @Cacheable(value = "stock_info", key = "#isinCode", unless = "#result == null")
    public StockWellnessDetail getSingleStockDetail(String isinCode) {
        return loadStockPricePort.findLateststockPrice(isinCode)
                .map(stockPrice -> convertToDetail(stockPrice, loadTechnicalDataPort.loadTechnicalContext(isinCode)))
                .orElse(null);
    }

    private StockWellnessDetail convertToDetail(StockPrice stockPrice, AiAnalysisContext context) {
        String rsiStatus = TechnicalCalculator.analyzeRsiLevel(stockPrice.getIndicators().getRsi14());
        String aiInsight = null;

        return null;
    }
}
