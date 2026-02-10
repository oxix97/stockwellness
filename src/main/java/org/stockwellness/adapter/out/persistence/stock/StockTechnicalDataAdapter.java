package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.stock.repository.StockHistoryRepository;
import org.stockwellness.application.port.out.stock.LoadTechnicalDataPort;
import org.stockwellness.domain.stock.StockHistory;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.analysis.MarketCondition;
import org.stockwellness.domain.stock.analysis.TechnicalCalculator;
import org.stockwellness.domain.stock.exception.StockDataNotFoundException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockTechnicalDataAdapter implements LoadTechnicalDataPort {

    private static final int RECENT_HISTORY_LIMIT = 2;
    private final StockHistoryRepository stockHistoryRepository;

    @Override
    public AiAnalysisContext loadTechnicalContext(String isinCode) {
        List<StockHistory> histories = stockHistoryRepository.findRecentHistory(isinCode, LocalDate.now(), RECENT_HISTORY_LIMIT);

        if (histories.isEmpty()) {
            throw new StockDataNotFoundException(isinCode);
        }

        StockHistory today = histories.get(0);
        StockHistory yesterday = histories.size() > 1 ? histories.get(1) : null;

        MarketCondition condition = TechnicalCalculator.analyze(today, yesterday);

        return AiAnalysisContext.of(today, condition);
    }

    @Override
    public Map<String, AiAnalysisContext> loadTechnicalContexts(List<String> isinCodes) {
        Map<String, List<StockHistory>> historiesMap = stockHistoryRepository.findRecentHistoryBatch(isinCodes, RECENT_HISTORY_LIMIT);

        return historiesMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            List<StockHistory> histories = e.getValue();
                            StockHistory today = histories.get(0);
                            StockHistory yesterday = histories.size() > 1 ? histories.get(1) : null;
                            MarketCondition condition = TechnicalCalculator.analyze(today, yesterday);
                            return AiAnalysisContext.of(today, condition);
                        }
                ));
    }
}