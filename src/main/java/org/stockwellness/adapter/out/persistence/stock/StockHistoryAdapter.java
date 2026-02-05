package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.persistence.stock.repository.StockHistoryRepository;
import org.stockwellness.application.port.out.stock.LoadStockHistoryPort;
import org.stockwellness.domain.stock.StockHistory;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StockHistoryAdapter implements LoadStockHistoryPort {

    private final StockHistoryRepository stockHistoryRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<StockHistory> findLatestHistory(String isinCode) {
        return stockHistoryRepository.findTopByIsinCodeOrderByBaseDateDesc(isinCode);
    }

    @Override
    public List<StockHistory> loadRecentHistories(String isinCode, int limit) {
        return stockHistoryRepository.findRecentHistory(isinCode, LocalDate.now(), limit);
    }

    @Override
    public Map<String, List<StockHistory>> loadRecentHistoriesBatch(List<String> isinCodes, int limit) {
        return stockHistoryRepository.findRecentHistoryBatch(isinCodes, limit);
    }

    /**
     * PostgreSQL Bulk Insert (Upsert 적용)
     * ON CONFLICT 구문을 사용하여 재실행 시에도 안전하게 업데이트
     */

    public void bulkInsert(List<StockHistory> histories) {
        String sql = """
                    INSERT INTO stock_history 
                    (isin_code, base_date, close_price, open_price, high_price, low_price, 
                     price_change, fluctuation_rate, volume, trading_value, market_cap, 
                     ma_5, ma_20, rsi_14)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (isin_code, base_date) 
                    DO UPDATE SET
                        close_price = EXCLUDED.close_price,
                        volume = EXCLUDED.volume,
                        market_cap = EXCLUDED.market_cap,
                        fluctuation_rate = EXCLUDED.fluctuation_rate
                """;

        jdbcTemplate.batchUpdate(sql, histories, 1000, (ps, history) -> {
            ps.setString(1, history.getIsinCode());
            ps.setDate(2, Date.valueOf(history.getBaseDate()));
            ps.setBigDecimal(3, history.getClosePrice());
            ps.setBigDecimal(4, history.getOpenPrice());
            ps.setBigDecimal(5, history.getHighPrice());
            ps.setBigDecimal(6, history.getLowPrice());
            ps.setBigDecimal(7, history.getPriceChange());
            ps.setBigDecimal(8, history.getFluctuationRate());
            ps.setLong(9, history.getVolume());
            ps.setBigDecimal(10, history.getTradingValue());
            ps.setBigDecimal(11, history.getMarketCap());
            // 지표 데이터 (초기 적재 시에는 null일 수 있음)
            ps.setBigDecimal(12, history.getMa5());
            ps.setBigDecimal(13, history.getMa20());
            ps.setBigDecimal(14, history.getRsi14());
        });
    }
}
