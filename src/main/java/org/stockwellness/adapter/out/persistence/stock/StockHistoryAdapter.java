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
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StockHistoryAdapter implements LoadStockHistoryPort {

    private final StockHistoryRepository historyJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<StockHistory> findLatestHistory(String isinCode) {
        return historyJpaRepository.findTopByIsinCodeOrderByBaseDateDesc(isinCode);
    }

    public List<StockHistory> findTop150ByIsinCodeAndBaseDateOrderByBaseDateDesc(String isinCode, LocalDate baseDate) {
        return historyJpaRepository.findTop150ByIsinCodeAndBaseDateOrderByBaseDateDesc(isinCode, baseDate);
    }


    public List<StockHistory> findTop60ByIsinCodeAndBaseDateBeforeOrderByBaseDateAsc(String isinCode, LocalDate baseDate) {
        return historyJpaRepository.findTop60ByIsinCodeAndBaseDateBeforeOrderByBaseDateAsc(isinCode, baseDate);
    }


    public List<String> findAllIsinCodes() {
        return historyJpaRepository.findAllIsinCodes();
    }


    public List<StockHistory> findTop2ByIsinCodeOrderByBaseDateDesc(String isinCode) {
        return historyJpaRepository.findTop2ByIsinCodeOrderByBaseDateDesc(isinCode);
    }


    public List<StockHistory> findAllByIsinCodeOrderByBaseDateAsc(String isinCode) {
        return historyJpaRepository.findAllByIsinCodeOrderByBaseDateAsc(isinCode);
    }


    public List<StockHistory> findByIsinCodeAndBaseDateBetweenOrderByBaseDateAsc(String isinCode, LocalDate startDate, LocalDate endDate) {
        return historyJpaRepository.findByIsinCodeAndBaseDateBetweenOrderByBaseDateAsc(isinCode, startDate, endDate);
    }


    public Optional<StockHistory> findTopByIsinCodeOrderByBaseDateDesc(String isinCode) {
        return historyJpaRepository.findTopByIsinCodeOrderByBaseDateDesc(isinCode);
    }


    public List<StockHistory> findByBaseDate(LocalDate baseDate) {
        return historyJpaRepository.findByBaseDate(baseDate);
    }


    public List<StockHistory> findRecentHistory(String isinCode, LocalDate targetDate, int limit) {
        return historyJpaRepository.findRecentHistory(isinCode, targetDate, limit);
    }


    public void saveAll(List<StockHistory> histories) {
        historyJpaRepository.saveAll(histories);
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
