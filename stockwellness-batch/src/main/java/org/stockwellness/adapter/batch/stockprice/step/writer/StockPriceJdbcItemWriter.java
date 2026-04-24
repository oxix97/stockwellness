package org.stockwellness.adapter.batch.stockprice.step.writer;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.stockwellness.domain.stock.price.StockPrice;

@RequiredArgsConstructor
public class StockPriceJdbcItemWriter implements ItemWriter<StockPrice> {

    private final JdbcBatchItemWriter<StockPrice> stockPriceJdbcWriter;

    @Override
    public void write(Chunk<? extends StockPrice> chunk) throws Exception {
        if (chunk.isEmpty()) {
            return;
        }

        stockPriceJdbcWriter.write(new Chunk<>(chunk.getItems()));
    }
}
