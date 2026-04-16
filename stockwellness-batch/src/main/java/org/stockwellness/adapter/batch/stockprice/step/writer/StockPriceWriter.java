package org.stockwellness.adapter.batch.stockprice.step.writer;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.price.StockPrice;

import java.util.ArrayList;

@RequiredArgsConstructor
public class StockPriceWriter implements ItemWriter<StockPrice> {

    private final StockPricePort stockPricePort;

    @Override
    public void write(Chunk<? extends StockPrice> chunk) {
        stockPricePort.saveAll(new ArrayList<>(chunk.getItems()));
    }
}