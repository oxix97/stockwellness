package org.stockwellness.adapter.out.persistence.stock;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.out.stock.EodStockInvestorTradePort;
import org.stockwellness.domain.stock.price.EodStockInvestorTradeDaily;
import org.stockwellness.domain.stock.price.InvestorTradeRanking;
import org.stockwellness.domain.stock.price.InvestorType;

@Component
@RequiredArgsConstructor
public class EodStockInvestorTradeAdapter implements EodStockInvestorTradePort {
    private static final String UPSERT = """
            INSERT INTO stock_investor_trade_daily AS existing
                (stock_id, trade_date, quote_scope, investor_type, net_quantity, net_amount,
                 data_status, source, collected_at, revision)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
            ON CONFLICT (stock_id, trade_date, quote_scope, investor_type) DO UPDATE SET
                net_quantity = EXCLUDED.net_quantity,
                net_amount = EXCLUDED.net_amount,
                data_status = EXCLUDED.data_status,
                source = EXCLUDED.source,
                collected_at = EXCLUDED.collected_at,
                revision = existing.revision + CASE WHEN
                    (existing.net_quantity, existing.net_amount, existing.data_status, existing.source)
                    IS DISTINCT FROM
                    (EXCLUDED.net_quantity, EXCLUDED.net_amount, EXCLUDED.data_status, EXCLUDED.source)
                    THEN 1 ELSE 0 END,
                corrected_at = CASE WHEN
                    (existing.net_quantity, existing.net_amount, existing.data_status, existing.source)
                    IS DISTINCT FROM
                    (EXCLUDED.net_quantity, EXCLUDED.net_amount, EXCLUDED.data_status, EXCLUDED.source)
                    THEN EXCLUDED.collected_at ELSE existing.corrected_at END
            WHERE CASE existing.data_status
                WHEN 'FINAL' THEN EXCLUDED.data_status = 'FINAL'
                WHEN 'PROVISIONAL' THEN EXCLUDED.data_status IN ('PROVISIONAL', 'FINAL')
                ELSE TRUE
            END
            """;

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void upsert(List<EodStockInvestorTradeDaily> values) {
        if (values == null || values.isEmpty()) return;
        jdbc.batchUpdate(UPSERT, values, values.size(), EodStockInvestorTradeAdapter::setValues);
    }

    @Override
    public List<InvestorTradeRanking> findRanked(LocalDate tradeDate, String quoteScope,
            InvestorType type, int limit) {
        if (tradeDate == null || quoteScope == null || quoteScope.isBlank() || type == null || limit < 1) {
            throw new IllegalArgumentException("Date, quote scope, investor type, and positive limit are required");
        }
        String investorTypes = type == InvestorType.ALL
                ? "'FOREIGN', 'INSTITUTION_TOTAL'"
                : "'" + (type == InvestorType.FOREIGN ? "FOREIGN" : "INSTITUTION_TOTAL") + "'";
        int requiredTypes = type == InvestorType.ALL ? 2 : 1;
        String sql = """
                SELECT stock_id, SUM(net_quantity) AS net_quantity, SUM(net_amount) AS net_amount
                FROM stock_investor_trade_daily
                WHERE trade_date = ? AND quote_scope = ? AND investor_type IN (%s)
                  AND data_status = 'FINAL' AND net_quantity IS NOT NULL AND net_amount IS NOT NULL
                GROUP BY stock_id
                HAVING COUNT(DISTINCT investor_type) = ?
                ORDER BY SUM(net_amount) DESC, stock_id
                LIMIT ?
                """.formatted(investorTypes);
        return jdbc.query(sql, (rs, rowNum) -> new InvestorTradeRanking(
                rs.getLong("stock_id"), rs.getBigDecimal("net_quantity"), rs.getBigDecimal("net_amount")),
                tradeDate, quoteScope, requiredTypes, limit);
    }

    private static void setValues(PreparedStatement ps, EodStockInvestorTradeDaily row) throws SQLException {
        int index = 1;
        ps.setLong(index++, row.stockId());
        ps.setDate(index++, java.sql.Date.valueOf(row.tradeDate()));
        ps.setString(index++, row.quoteScope());
        ps.setString(index++, row.investorType().name());
        ps.setBigDecimal(index++, row.netQuantity());
        ps.setBigDecimal(index++, row.netAmount());
        ps.setString(index++, row.dataStatus());
        ps.setString(index++, row.source());
        ps.setTimestamp(index, Timestamp.from(row.collectedAt()));
    }
}
