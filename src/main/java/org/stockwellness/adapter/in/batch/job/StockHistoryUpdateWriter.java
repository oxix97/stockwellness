package org.stockwellness.adapter.in.batch.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.domain.stock.StockHistory;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StockHistoryUpdateWriter implements ItemWriter<List<StockHistory>> {

    private final JdbcTemplate jdbcTemplate;

    private static final String UPDATE_SQL = """
        UPDATE stock_history 
        SET ma_5 = ?, ma_20 = ?, ma_60 = ?, ma_120 = ?, rsi_14 = ?, macd = ?, updated_at = CURRENT_DATE
        WHERE isin_code = ? AND base_date = ?
    """;

    @Override
    @Transactional
    public void write(Chunk<? extends List<StockHistory>> chunk) {
        // Chunk<List<StockHistory>> -> List<StockHistory> (Flattening)
        List<StockHistory> allHistories = new ArrayList<>();
        chunk.getItems().forEach(allHistories::addAll);

        // JDBC Batch Update 실행
        jdbcTemplate.batchUpdate(UPDATE_SQL, allHistories, 1000, (ps, history) -> {
            ps.setBigDecimal(1, history.getMa5());
            ps.setBigDecimal(2, history.getMa20());
            ps.setBigDecimal(3, history.getMa60());
            ps.setBigDecimal(4, history.getMa120());
            ps.setBigDecimal(5, history.getRsi14());
            ps.setBigDecimal(6, history.getMacd());
            ps.setString(7, history.getIsinCode());
            ps.setDate(8, Date.valueOf(history.getBaseDate()));
        });
    }
}