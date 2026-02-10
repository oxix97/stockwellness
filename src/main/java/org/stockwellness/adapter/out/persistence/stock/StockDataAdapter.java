package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.LoadStockHistoryPort;
import org.stockwellness.application.port.out.stock.LoadTechnicalDataPort;
import org.stockwellness.application.port.out.watchlist.StockDataPort;
import org.stockwellness.domain.stock.StockHistory;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.analysis.TechnicalCalculator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StockDataAdapter implements StockDataPort {

    private final LoadStockHistoryPort loadStockHistoryPort;
    private final LoadTechnicalDataPort loadTechnicalDataPort;

    @Override
    public Map<String, StockWellnessDetail> getStockDetails(List<String> isinCodes) {
        Map<String, List<StockHistory>> latestHistories = loadStockHistoryPort.loadRecentHistoriesBatch(isinCodes, 1);
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
        return loadStockHistoryPort.findLatestHistory(isinCode)
                .map(history -> convertToDetail(history, loadTechnicalDataPort.loadTechnicalContext(isinCode)))
                .orElse(null);
    }

    private StockWellnessDetail convertToDetail(StockHistory history, AiAnalysisContext context) {
        String rsiStatus = TechnicalCalculator.analyzeRsiLevel(history.getRsi14());
        String aiInsight = (context != null) ?
                String.format("현재 %s 종목은 %s 상태이며, RSI 수치는 %s입니다.",
                        history.getIsinCode(),
                        context.technicalSignal().trendStatus().getDescription(),
                        history.getRsi14()) :
                "데이터가 부족하여 AI 분석을 제공할 수 없습니다.";

        return new StockWellnessDetail(
                history.getIsinCode(),
                history.getClosePrice(),
                history.getFluctuationRate(),
                history.getRsi14(),
                rsiStatus,
                aiInsight
        );
    }
}
