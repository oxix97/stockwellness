package org.stockwellness.adapter.in.batch.step;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.stereotype.Component;
import org.stockwellness.domain.stock.StockHistory;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class StockHistoryWriter implements ItemWriter<List<StockHistory>> {

    private final JdbcBatchItemWriter<StockHistory> delegate;

    public StockHistoryWriter(DataSource dataSource) {
        this.delegate = new JdbcBatchItemWriterBuilder<StockHistory>()
                .dataSource(dataSource)
                .sql("UPDATE stock_history " +
                        "SET ma_5 = :ma5, " +
                        "    ma_20 = :ma20, " +
                        "    ma_60 = :ma60, " +
                        "    ma_120 = :ma120, " +
                        "    rsi_14 = :rsi14, " +
                        "    macd = :macd " +
                        "WHERE isin_code = :isinCode " +
                        "  AND base_date = :baseDate")
                .beanMapped()
                .assertUpdates(false)
                .build();

        this.delegate.afterPropertiesSet();
    }

    @Override
    public void write(Chunk<? extends List<StockHistory>> chunk) throws Exception {
        List<StockHistory> flattenedList = new ArrayList<>();

        for (List<StockHistory> list : chunk) {
            if (list != null && !list.isEmpty()) {
                flattenedList.addAll(list);
            }
        }

        if (flattenedList.isEmpty()) {
            return;
        }

        log.debug("Updating indicators for {} rows...", flattenedList.size());

        delegate.write(new Chunk<>(flattenedList));
    }
}