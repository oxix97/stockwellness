package org.stockwellness.adapter.batch.stockprice.step.processor;


import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;
import org.stockwellness.application.port.out.external.kis.KisDailyPricePort;
import org.stockwellness.adapter.out.external.kis.dto.KisMultiStockPriceDetail;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DailyStockPriceProcessor implements ItemProcessor<List<Stock>, List<StockPrice>> {

    private final KisDailyPricePort kisDailyPricePort;
    private final LocalDate targetDate;

    @Nullable
    @Override
    public List<StockPrice> process(@NonNull List<Stock> stocks) {
        Map<String, KisMultiStockPriceDetail> priceMap = kisDailyPricePort.fetchMultiStockPrices(
                        stocks.stream()
                                .map(Stock::getTicker)
                                .toList()
                ).stream()
                .collect(Collectors.toMap(
                        KisMultiStockPriceDetail::ticker,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        return stocks.stream()
                .map(stock -> {
                    KisMultiStockPriceDetail dto = priceMap.get(stock.getTicker());
                    return dto == null ? null : mapToStockPrice(stock, dto);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private StockPrice mapToStockPrice(
            Stock stock,
            KisMultiStockPriceDetail dto
    ) {
        return StockPrice.of(
                stock,
                targetDate,
                dto.openPrice(),
                dto.highPrice(),
                dto.lowPrice(),
                dto.closePrice(),
                dto.closePrice(),
                dto.previousClosePrice(),
                dto.accumulatedVolume(),
                dto.accumulatedTradingValue(),
                TechnicalIndicators.empty()
        );
    }
}
