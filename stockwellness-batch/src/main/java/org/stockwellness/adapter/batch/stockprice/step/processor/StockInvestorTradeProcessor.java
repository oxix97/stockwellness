package org.stockwellness.adapter.batch.stockprice.step.processor;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.KisMultiStockPriceDetail;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockInvestorTrade;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class StockInvestorTradeProcessor implements ItemProcessor<List<Stock>, List<StockInvestorTrade>> {

    private final KisDailyPriceAdapter kisDailyPriceAdapter;

    @Nullable
    @Override
    public List<StockInvestorTrade> process(@NonNull List<Stock> stocks) {
        Map<String, KisMultiStockPriceDetail> priceMap = kisDailyPriceAdapter.fetchMultiStockPrices(
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
                    return dto == null ? null : mapToStockInvestorTrade(stock, dto);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private static StockInvestorTrade mapToStockInvestorTrade(
            Stock stock,
            KisMultiStockPriceDetail dto
    ) {
        return StockInvestorTrade.of(
                stock,
                LocalDate.now(),
                stock.getName(),
                stock.getTicker(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                dto.netForeignBuyingAmt(),
                dto.netInstitutionalBuyingAmt(),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
