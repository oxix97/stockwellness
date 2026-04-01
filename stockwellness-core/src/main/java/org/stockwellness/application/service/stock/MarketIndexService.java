package org.stockwellness.application.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.stock.MarketIndexUseCase;
import org.stockwellness.application.port.in.stock.result.MarketIndexResult;
import org.stockwellness.application.port.in.stock.result.MarketIndexResult.HistoryPoint;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.domain.stock.exception.StockPriceException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketIndexService implements MarketIndexUseCase {

    private final LoadBenchmarkPort loadBenchmarkPort;

    private static final int HISTORY_DAYS = 30;
    private static final int DISPLAY_SCALE = 2;

    @Override
    public List<MarketIndexResult> getMarketIndexes() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(HISTORY_DAYS);

        List<MarketIndexResult> results = new ArrayList<>();

        for (BenchmarkType type : BenchmarkType.values()) {
            try {
                // BenchmarkType의 ticker()를 식별자로 사용하여 데이터 로드
                List<StockPriceResult> prices = loadBenchmarkPort.loadBenchmarkPrices(type.getTicker(), start, end);
                results.add(toResult(type.getName(), prices));
            } catch (StockPriceException e) {
                log.warn("[지수 서비스] 시장 지수 조회 실패: {} - {}", type.getName(), e.getMessage());
                results.add(emptyResult(type.getName()));
            } catch (Exception e) {
                log.error("[지수 서비스] 시장 지수 조회 중 예기치 않은 오류 발생: {} - {}", type.getName(), e.getMessage());
                results.add(emptyResult(type.getName()));
            }
        }
        return results;
    }

    private MarketIndexResult toResult(String name, List<StockPriceResult> prices) {
        if (prices.isEmpty()) {
            return emptyResult(name);
        }

        StockPriceResult latest = prices.get(prices.size() - 1);
        BigDecimal currentPrice = latest.closePrice();
        BigDecimal fluctuationRate = latest.changeRate() != null ? 
                latest.changeRate().setScale(DISPLAY_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        
        // fluctuationAmount = currentPrice - prevPrice
        // prevPrice = currentPrice / (1 + fluctuationRate/100)
        BigDecimal fluctuationAmount = BigDecimal.ZERO;
        if (fluctuationRate.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal rateMultiplier = fluctuationRate.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
            BigDecimal divisor = BigDecimal.ONE.add(rateMultiplier);
            
            if (divisor.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal prevPrice = currentPrice.divide(divisor, 4, RoundingMode.HALF_UP);
                fluctuationAmount = currentPrice.subtract(prevPrice);
            }
        }

        List<HistoryPoint> history = prices.stream()
                .map(p -> new HistoryPoint(p.baseDate(), p.closePrice()))
                .toList();

        return new MarketIndexResult(name, currentPrice, fluctuationRate, fluctuationAmount, history);
    }

    private MarketIndexResult emptyResult(String name) {
        return new MarketIndexResult(name, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Collections.emptyList());
    }
}
