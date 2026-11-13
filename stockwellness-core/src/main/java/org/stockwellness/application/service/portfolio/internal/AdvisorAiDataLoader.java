package org.stockwellness.application.service.portfolio.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.portfolio.AdvisorAiContext;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.IndexDailyReadPort;
import org.stockwellness.application.port.out.stock.LoadTechnicalDataPort;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.domain.stock.exception.StockDataNotFoundException;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;

@Component
@RequiredArgsConstructor
public class AdvisorAiDataLoader {

    private final PortfolioPort portfolioPort;
    private final LoadTechnicalDataPort loadTechnicalDataPort;
    private final IndexDailyReadPort indexDailyReadPort;

    public AdvisorAiContext loadContext(Long portfolioId) {
        Portfolio portfolio = portfolioPort.findById(portfolioId)
                .orElseThrow(PortfolioNotFoundException::new);

        List<String> tickers = portfolio.getItems().stream()
                .filter(item -> item.getAssetType() == AssetType.STOCK)
                .map(PortfolioItem::getSymbol)
                .toList();

        var technicalMap = loadTechnicalDataPort.loadTechnicalContexts(tickers);
        for (String ticker : tickers) {
            if (!technicalMap.containsKey(ticker)) throw new StockDataNotFoundException(ticker);
        }

        BigDecimal totalPurchaseAmount = portfolio.calculateTotalPurchaseAmount();

        List<AdvisorAiContext.HoldingInfo> holdings = portfolio.getItems().stream()
                .filter(item -> item.getAssetType() == AssetType.STOCK)
                .map(item -> {
                    var input = technicalMap.get(item.getSymbol());
                    AiAnalysisContext tech = input.context();
                    
                    BigDecimal currentWeight = totalPurchaseAmount.compareTo(BigDecimal.ZERO) > 0
                            ? item.calculatePurchaseAmount().divide(totalPurchaseAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;

                    return new AdvisorAiContext.HoldingInfo(
                            item.getSymbol(),
                            input.stockName(),
                            item.getQuantity(),
                            tech.priceInfo().closePrice(),
                            currentWeight,
                            item.getTargetWeight(),
                            tech
                    );
                })
                .toList();

        // 벤치마크 데이터 로드 (최근 1일 기준 등락률 포함)
        var benchmarkPrices = indexDailyReadPort.recentFinal("0001", LocalDate.now(), 2);
        List<AdvisorAiContext.MarketBenchmark> benchmarks = List.of();
        if (!benchmarkPrices.isEmpty()) {
            var latest = benchmarkPrices.getFirst();
            var prev = benchmarkPrices.size() > 1 ? benchmarkPrices.get(1) : null;
            
            BigDecimal flucRate = null;
            if (prev != null && prev.closePrice().compareTo(BigDecimal.ZERO) > 0) {
                flucRate = latest.closePrice().subtract(prev.closePrice())
                        .divide(prev.closePrice(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            }

            benchmarks = List.of(new AdvisorAiContext.MarketBenchmark("KOSPI", latest.closePrice(), flucRate));
        }

        return new AdvisorAiContext(portfolio.getName(), holdings, benchmarks);
    }
}
