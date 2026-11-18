package org.stockwellness.adapter.batch.stockprice.step.writer;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.stockwellness.application.port.in.batch.NewMarketDataSyncUseCase;
import org.stockwellness.domain.stock.Stock;

@RequiredArgsConstructor
public class StockPriceListWriter implements ItemWriter<java.util.List<Stock>> {
    private final NewMarketDataSyncUseCase sync;
    private final LocalDate tradeDate;

    @Override
    public void write(Chunk<? extends java.util.List<Stock>> chunk) {
        for (java.util.List<Stock> stocks : chunk) {
            for (Stock stock : stocks) sync.syncStockPrices(stock, tradeDate, tradeDate);
        }
    }
}
