package org.stockwellness.adapter.in.batch.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.persistence.stock.StockHistoryAdapter;
import org.stockwellness.domain.stock.StockHistory;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StockPriceWriter implements ItemWriter<List<StockHistory>> {

    private final StockHistoryAdapter historyAdapter;

    @Override
    public void write(Chunk<? extends List<StockHistory>> chunk) {
        for (List<StockHistory> dailyHistories : chunk) {
            if (dailyHistories != null && !dailyHistories.isEmpty()) {
                historyAdapter.bulkInsert(dailyHistories);
            }
        }
    }
}