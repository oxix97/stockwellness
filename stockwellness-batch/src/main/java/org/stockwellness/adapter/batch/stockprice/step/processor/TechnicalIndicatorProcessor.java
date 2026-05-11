package org.stockwellness.adapter.batch.stockprice.step.processor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.analysis.TechnicalIndicatorCalculator;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

@Component
@RequiredArgsConstructor
public class TechnicalIndicatorProcessor implements ItemProcessor<Stock, StockPrice> {

    private static final int REQUIRED_PRICE_COUNT = 5;
    private static final int LOOKBACK_DAYS = 120;

    private final StockPricePort stockPricePort;

    @Override
    public StockPrice process(Stock stock) {
        List<StockPrice> prices = stockPricePort.findRecentPricesWithDateByStock(
                stock,
                LocalDate.now(),
                LOOKBACK_DAYS
        );

        if (prices == null || prices.size() < REQUIRED_PRICE_COUNT) {
            return null;
        }

        // 레포지토리에서 DESC로 가져오므로 계산 전에 ASC로 정렬한다.
        prices.sort(Comparator.comparing(price -> price.getId().getBaseDate()));

        List<BigDecimal> closingPrices = prices.stream()
                .map(StockPrice::getClosePrice)
                .toList();

        TechnicalIndicators latestIndicators = TechnicalIndicatorCalculator.calculateLatest(closingPrices);

        StockPrice latestPrice = prices.getLast();
        latestPrice.updateIndicators(latestIndicators);
        return latestPrice;
    }
}
