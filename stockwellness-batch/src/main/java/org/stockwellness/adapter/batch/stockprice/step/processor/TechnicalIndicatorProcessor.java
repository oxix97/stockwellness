package org.stockwellness.adapter.batch.stockprice.step.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.analysis.TechnicalIndicatorCalculator;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TechnicalIndicatorProcessor implements ItemProcessor<List<Stock>, List<StockPrice>> {

    private static final int REQUIRED_PRICE_COUNT = 5;
    private static final int LOOKBACK_DAYS = 120;

    private final StockPricePort stockPricePort;

    @Override
    public List<StockPrice> process(List<Stock> stocks) {
        // [N+1 해결] 청크 단위로 최근 시세를 일괄 조회
        Map<Long, List<StockPrice>> stockPriceMap = stockPricePort.findRecentPricesWithDateByStocks(
                stocks, 
                LocalDate.now(), 
                LOOKBACK_DAYS
        );

        List<StockPrice> processedPrices = new ArrayList<>();

        for (Stock stock : stocks) {
            List<StockPrice> prices = stockPriceMap.get(stock.getId());

            if (prices == null || prices.size() < REQUIRED_PRICE_COUNT) {
                continue;
            }

            // 날짜 순 정렬 (레포지토리에서 DESC로 가져오므로 뒤집기 위해 정렬)
            prices.sort(Comparator.comparing(price -> price.getId().getBaseDate()));

            List<BigDecimal> closingPrices = prices.stream()
                    .map(StockPrice::getClosePrice)
                    .toList();

            TechnicalIndicators latestIndicators =
                    TechnicalIndicatorCalculator.calculateLatest(closingPrices);

            // 가장 최근 시세에 지표 업데이트
            StockPrice latestPrice = prices.getLast();
            latestPrice.updateIndicators(latestIndicators);
            
            processedPrices.add(latestPrice);
        }

        return processedPrices.isEmpty() ? null : processedPrices;
    }
}
