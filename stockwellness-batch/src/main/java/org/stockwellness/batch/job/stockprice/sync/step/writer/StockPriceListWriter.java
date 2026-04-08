package org.stockwellness.batch.job.stockprice.sync.step.writer;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.global.util.DateUtil;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class StockPriceListWriter implements ItemWriter<List<StockPrice>> {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcBatchItemWriter<StockPrice> stockPriceJdbcWriter;

    @Override
    public void write(Chunk<? extends List<StockPrice>> chunk) throws Exception {
        List<StockPrice> flatList = new ArrayList<>();
        for (List<StockPrice> list : chunk) {
            if (list != null) {
                flatList.addAll(list);
            }
        }

        if (flatList.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                "DELETE FROM stock_price WHERE base_date = CAST(? AS date) AND stock_id = CAST(? AS bigint)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        StockPrice stockPrice = flatList.get(i);
                        ps.setDate(1, DateUtil.toSqlDate(stockPrice.getId().getBaseDate()));
                        ps.setLong(2, stockPrice.getId().getStockId());
                    }

                    @Override
                    public int getBatchSize() {
                        return flatList.size();
                    }
                }
        );

        stockPriceJdbcWriter.write(new Chunk<>(flatList));
    }
}
