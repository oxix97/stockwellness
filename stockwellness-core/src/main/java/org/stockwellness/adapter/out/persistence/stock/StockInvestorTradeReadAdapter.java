package org.stockwellness.adapter.out.persistence.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.stock.result.StockSupplyRankingResult;
import org.stockwellness.application.port.out.stock.StockInvestorTradeReadPort;
import org.stockwellness.domain.stock.price.TradeDirection;

@Component
@RequiredArgsConstructor
public class StockInvestorTradeReadAdapter implements StockInvestorTradeReadPort {
    private final JdbcTemplate jdbc;

    @Override
    public Optional<LocalDate> latestFinalDate(String quoteScope) {
        if (quoteScope == null || quoteScope.isBlank()) throw new IllegalArgumentException("Quote scope is required");
        return Optional.ofNullable(jdbc.query("""
                SELECT MAX(trade_date)
                FROM (
                    SELECT trade_date, stock_id
                    FROM stock_investor_trade_daily
                    WHERE quote_scope = ? AND data_status = 'FINAL'
                      AND investor_type IN ('FOREIGN', 'INSTITUTION_TOTAL')
                    GROUP BY trade_date, stock_id
                    HAVING COUNT(DISTINCT investor_type) = 2
                ) complete_dates
                """, rs -> rs.next() ? rs.getObject(1, LocalDate.class) : null, quoteScope));
    }

    @Override
    public List<StockSupplyRankingResult> topStocks(LocalDate tradeDate, String quoteScope, String investorType,
            TradeDirection direction, int limit) {
        if (tradeDate == null || quoteScope == null || quoteScope.isBlank() || direction == null || limit < 1
                || !List.of("FOREIGN", "INSTITUTION_TOTAL").contains(investorType)) {
            throw new IllegalArgumentException("A valid final investor ranking query is required");
        }
        String order = direction == TradeDirection.BUY ? "DESC" : "ASC";
        return jdbc.query("""
                WITH final_prices AS (
                    SELECT stock_id, trade_date, close_price, trading_amount,
                           LAG(close_price) OVER (PARTITION BY stock_id, quote_scope ORDER BY trade_date) AS previous_close
                    FROM stock_price_daily
                    WHERE quote_scope = ? AND data_status = 'FINAL' AND trade_date <= ?
                ), latest_prices AS (
                    SELECT DISTINCT ON (stock_id) stock_id, trade_date, close_price, trading_amount, previous_close
                    FROM final_prices
                    ORDER BY stock_id, trade_date DESC
                )
                SELECT stock.ticker, stock.name AS stock_name, sector.name AS sector_name,
                       price.close_price AS current_price,
                       CASE WHEN price.close_price IS NULL OR price.previous_close IS NULL
                                  OR price.previous_close <= 0 THEN NULL
                            ELSE (price.close_price - price.previous_close) / price.previous_close * 100
                       END AS fluctuation_rate,
                       flow.net_quantity, flow.net_amount, price.trading_amount AS transaction_amount
                FROM stock_investor_trade_daily flow
                JOIN stock ON stock.id = flow.stock_id
                LEFT JOIN market_index sector ON sector.id = stock.sector_index_id
                LEFT JOIN latest_prices price ON price.stock_id = flow.stock_id
                WHERE flow.trade_date = ? AND flow.quote_scope = ? AND flow.investor_type = ?
                  AND flow.data_status = 'FINAL'
                  AND ((? = 'BUY' AND flow.net_amount > 0) OR (? = 'SELL' AND flow.net_amount < 0))
                ORDER BY flow.net_amount %s, stock.ticker ASC
                LIMIT ?
                """.formatted(order), (rs, rowNum) -> new StockSupplyRankingResult(
                rs.getString("ticker"), rs.getString("stock_name"), rs.getString("sector_name"),
                rs.getBigDecimal("current_price"), rs.getBigDecimal("fluctuation_rate"),
                exactQuantity(rs.getBigDecimal("net_quantity")), rs.getBigDecimal("net_amount"),
                rs.getBigDecimal("transaction_amount")),
                quoteScope, tradeDate, tradeDate, quoteScope, investorType,
                direction.name(), direction.name(), limit);
    }

    private static Long exactQuantity(BigDecimal quantity) {
        return quantity == null ? null : quantity.longValueExact();
    }
}
