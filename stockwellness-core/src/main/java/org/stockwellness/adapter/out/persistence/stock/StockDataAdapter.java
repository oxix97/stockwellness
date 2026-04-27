package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.application.port.out.stock.LoadTechnicalDataPort;
import org.stockwellness.application.port.out.watchlist.StockDataPort;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.analysis.TechnicalCalculator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StockDataAdapter implements StockDataPort {

    private final StockPricePort stockPricePort;
    private final LoadTechnicalDataPort loadTechnicalDataPort;

    @Override
    public Map<String, StockWellnessDetail> getStockDetails(List<String> isinCodes) {
        Map<String, List<StockPrice>> latestHistories = stockPricePort.loadRecentHistoriesBatch(isinCodes, 1);
        Map<String, AiAnalysisContext> technicalContexts = loadTechnicalDataPort.loadTechnicalContexts(isinCodes);

        return isinCodes.stream()
                .filter(isinCode -> latestHistories.containsKey(isinCode) && !latestHistories.get(isinCode).isEmpty())
                .collect(Collectors.toMap(
                        isinCode -> isinCode,
                        isinCode -> convertToDetail(
                                latestHistories.get(isinCode).getFirst(),
                                technicalContexts.get(isinCode)
                        )
                ));
    }

    public StockWellnessDetail getSingleStockDetail(String isinCode) {
        return stockPricePort.findLateststockPrice(isinCode)
                .map(stockPrice -> convertToDetail(stockPrice, loadTechnicalDataPort.loadTechnicalContext(isinCode)))
                .orElse(null);
    }

    private StockWellnessDetail convertToDetail(StockPrice stockPrice, AiAnalysisContext context) {
        BigDecimal rsi = stockPrice.getIndicators() != null ? stockPrice.getIndicators().getRsi14() : null;
        String rsiStatus = TechnicalCalculator.analyzeRsiLevel(rsi);

        // context가 null일 경우를 대비한 방어 로직 및 추세 정보 기반 insight 추출
        String aiInsight = (context != null && context.technicalSignal() != null)
                ? context.technicalSignal().trendStatus().getDescription()
                : "데이터가 부족하여 AI 분석을 제공할 수 없습니다.";

        return new StockWellnessDetail(
                stockPrice.getStock().getStandardCode(),
                stockPrice.getClosePrice(),
                stockPrice.getFluctuationRate(),
                rsi,
                rsiStatus,
                aiInsight
        );
    }
}
