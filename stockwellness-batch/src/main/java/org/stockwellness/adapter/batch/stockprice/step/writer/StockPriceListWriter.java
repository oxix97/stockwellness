package org.stockwellness.adapter.batch.stockprice.step.writer;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.stockwellness.domain.stock.price.StockPrice;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class StockPriceListWriter implements ItemWriter<List<StockPrice>> {

    private final JdbcBatchItemWriter<StockPrice> stockPriceJdbcWriter;

    @Override
    public void write(Chunk<? extends List<StockPrice>> chunk) throws Exception {
        List<StockPrice> flatPrices = new ArrayList<>();

        for (List<StockPrice> prices : chunk) {
            if (prices != null) {
                flatPrices.addAll(prices);
            }
        }

        if (flatPrices.isEmpty()) {
            return;
        }

        stockPriceJdbcWriter.write(new Chunk<>(flatPrices));
    }
}
