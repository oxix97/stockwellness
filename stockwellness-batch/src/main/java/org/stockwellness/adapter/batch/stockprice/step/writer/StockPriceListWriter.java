package org.stockwellness.adapter.batch.stockprice.step.writer;

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
        List<StockPrice> flatPrices = new ArrayList<>();

        for (List<StockPrice> prices : chunk) {
            if (prices != null) {
                flatPrices.addAll(prices);
            }
        }

        if (flatPrices.isEmpty()) {
            return;
        }

        // stock_price 행을 재생성한 뒤 최신 값으로 upsert한다.
        jdbcTemplate.batchUpdate(
                "DELETE FROM stock_price WHERE base_date = CAST(? AS date) AND stock_id = CAST(? AS bigint)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        StockPrice stockPrice = flatPrices.get(i);
                        ps.setDate(1, DateUtil.toSqlDate(stockPrice.getId().getBaseDate()));
                        ps.setLong(2, stockPrice.getId().getStockId());
                    }

                    @Override
                    public int getBatchSize() {
                        return flatPrices.size();
                    }
                }
        );

        stockPriceJdbcWriter.write(new Chunk<>(flatPrices));
    }
}
