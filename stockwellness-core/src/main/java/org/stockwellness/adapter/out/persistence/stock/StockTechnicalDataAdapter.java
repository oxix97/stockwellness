package org.stockwellness.adapter.out.persistence.stock;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.out.stock.LoadTechnicalDataPort;
import org.stockwellness.application.port.out.stock.LoadTechnicalDataPort.CanonicalStockAnalysisInput;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.analysis.MarketCondition;
import org.stockwellness.domain.stock.analysis.TechnicalCalculator;
import org.stockwellness.domain.stock.exception.StockDataNotFoundException;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockTechnicalDataAdapter implements LoadTechnicalDataPort {
    private static final String LOAD_FINAL_CONTEXTS = """
            -- shortcut: canonical stock collection currently writes KRX scope only; add other scopes with their source contract.
            WITH requested AS (
                SELECT id, ticker, name FROM stock
                WHERE ticker IN (:tickers) AND market_code IN ('KOSPI', 'KOSDAQ')
            ), latest_final AS (
                SELECT DISTINCT ON (price.stock_id)
                    price.stock_id, price.trade_date, price.quote_scope, price.close_price,
                    price.volume, price.revision, price.source
                FROM requested stock
                JOIN stock_price_daily price ON price.stock_id = stock.id
                WHERE price.quote_scope = 'KRX' AND price.data_status = 'FINAL'
                  AND price.trade_date <= :asOfDate
                ORDER BY price.stock_id, price.trade_date DESC
            ), previous_final AS (
                SELECT DISTINCT ON (previous.stock_id)
                    previous.stock_id, previous.trade_date, previous.quote_scope,
                    previous.close_price, previous.revision
                FROM latest_final current
                JOIN stock_price_daily previous
                  ON previous.stock_id = current.stock_id
                 AND previous.quote_scope = current.quote_scope
                 AND previous.trade_date < current.trade_date
                 AND previous.data_status = 'FINAL'
                ORDER BY previous.stock_id, previous.trade_date DESC
            ), required_indicator AS (
                SELECT stock_id, trade_date, quote_scope FROM latest_final
                UNION
                SELECT stock_id, trade_date, quote_scope FROM previous_final
            ), valid_indicator AS (
                SELECT indicator.*
                FROM stock_technical_indicator indicator
                JOIN required_indicator needed
                  ON needed.stock_id = indicator.stock_id
                 AND needed.trade_date = indicator.trade_date
                 AND needed.quote_scope = indicator.quote_scope
                WHERE indicator.price_basis = 'RAW'
                  AND indicator.input_manifest ->> 'source' = 'stock_price_daily'
                  AND indicator.input_manifest ->> 'sourceId' = indicator.stock_id::text
                  AND indicator.input_manifest ->> 'quoteScope' = indicator.quote_scope
                  AND indicator.input_manifest ->> 'priceBasis' = indicator.price_basis
                  AND indicator.input_manifest ->> 'targetDate' = indicator.trade_date::text
                  AND jsonb_typeof(indicator.input_manifest -> 'observations') = 'array'
                  AND EXISTS (
                      SELECT 1
                      FROM jsonb_array_elements(indicator.input_manifest -> 'observations') AS input(value)
                      WHERE input.value ->> 'date' = indicator.trade_date::text
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM jsonb_array_elements(indicator.input_manifest -> 'observations') AS input(value)
                      LEFT JOIN stock_price_daily source_price
                        ON source_price.stock_id = indicator.stock_id
                       AND source_price.quote_scope = indicator.quote_scope
                       AND source_price.trade_date::text = input.value ->> 'date'
                      WHERE source_price.stock_id IS NULL
                         OR source_price.data_status <> 'FINAL'
                         OR source_price.revision::text IS DISTINCT FROM input.value ->> 'revision'
                  )
            )
            SELECT current.stock_id, stock.ticker, stock.name, current.trade_date, current.quote_scope,
                   current.close_price, current.volume, current.revision,
                   indicator.ma5, indicator.ma20, indicator.ma60, indicator.ma120,
                   indicator.rsi14, indicator.macd, indicator.macd_signal,
                   previous.close_price AS previous_close,
                   previous_indicator.ma5 AS previous_ma5, previous_indicator.ma20 AS previous_ma20,
                   indicator.algorithm_version,
                   jsonb_build_object(
                       'stock_id', current.stock_id,
                       'ticker', stock.ticker,
                       'trade_date', current.trade_date,
                       'quote_scope', current.quote_scope,
                       'price_revision', current.revision,
                       'price_source', current.source,
                       'price_basis', indicator.price_basis,
                       'indicator_algorithm_version', indicator.algorithm_version,
                       'indicator_manifest', indicator.input_manifest,
                       'previous_final', CASE WHEN previous.trade_date IS NULL THEN NULL ELSE jsonb_build_object(
                           'trade_date', previous.trade_date,
                           'price_revision', previous.revision,
                           'indicator_manifest', previous_indicator.input_manifest
                       ) END
                   )::text AS input_manifest
            FROM latest_final current
            JOIN stock ON stock.id = current.stock_id
            JOIN valid_indicator indicator
              ON indicator.stock_id = current.stock_id
             AND indicator.trade_date = current.trade_date
             AND indicator.quote_scope = current.quote_scope
            LEFT JOIN previous_final previous ON previous.stock_id = current.stock_id
            LEFT JOIN valid_indicator previous_indicator
              ON previous_indicator.stock_id = previous.stock_id
             AND previous_indicator.trade_date = previous.trade_date
             AND previous_indicator.quote_scope = previous.quote_scope
            ORDER BY stock.ticker
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public CanonicalStockAnalysisInput loadTechnicalContext(String ticker) {
        CanonicalStockAnalysisInput input = loadTechnicalContexts(List.of(ticker)).get(ticker);
        if (input == null) throw new StockDataNotFoundException(ticker);
        return input;
    }

    @Override
    public Map<String, CanonicalStockAnalysisInput> loadTechnicalContexts(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) return Map.of();
        if (tickers.stream().anyMatch(ticker -> ticker == null || ticker.isBlank())) {
            throw new IllegalArgumentException("Stock tickers are required");
        }

        Map<String, CanonicalStockAnalysisInput> inputs = new HashMap<>();
        jdbc.query(LOAD_FINAL_CONTEXTS, Map.of("tickers", tickers,
                "asOfDate", LocalDate.now(ZoneId.of("Asia/Seoul"))), rs -> {
            CanonicalStockAnalysisInput input = toInput(rs);
            if (inputs.putIfAbsent(input.ticker(), input) != null) {
                throw new IllegalStateException("Stock ticker is ambiguous across markets: " + input.ticker());
            }
        });
        return Map.copyOf(inputs);
    }

    private static CanonicalStockAnalysisInput toInput(java.sql.ResultSet rs) throws java.sql.SQLException {
        BigDecimal ma5 = rs.getBigDecimal("ma5");
        BigDecimal ma20 = rs.getBigDecimal("ma20");
        BigDecimal ma60 = rs.getBigDecimal("ma60");
        BigDecimal ma120 = rs.getBigDecimal("ma120");
        BigDecimal rsi = rs.getBigDecimal("rsi14");
        MarketCondition condition = TechnicalCalculator.analyze(ma5, ma20, ma60, ma120,
                rs.getBigDecimal("previous_ma5"), rs.getBigDecimal("previous_ma20"));

        String ticker = rs.getString("ticker");
        LocalDate tradeDate = rs.getDate("trade_date").toLocalDate();
        BigDecimal close = rs.getBigDecimal("close_price");
        BigDecimal previousClose = rs.getBigDecimal("previous_close");
        long rawVolume = rs.getLong("volume");
        BigDecimal volume = rs.wasNull() ? null : BigDecimal.valueOf(rawVolume);
        AiAnalysisContext context = new AiAnalysisContext(
                ticker,
                tradeDate,
                new AiAnalysisContext.PriceSummary(close, fluctuationRate(close, previousClose), volume),
                new AiAnalysisContext.TechnicalSignal(condition.trendStatus(), rsi,
                        TechnicalCalculator.analyzeRsiLevel(rsi), rs.getBigDecimal("macd"), condition.signal(),
                        ma5, ma20, ma60, ma120),
                new AiAnalysisContext.PortfolioRisk(false, 0.0));

        return new CanonicalStockAnalysisInput(rs.getLong("stock_id"), ticker, rs.getString("name"), tradeDate,
                rs.getString("quote_scope"), rs.getInt("revision"), rs.getString("input_manifest"),
                rs.getString("algorithm_version"), context);
    }

    private static BigDecimal fluctuationRate(BigDecimal close, BigDecimal previousClose) {
        if (close == null || previousClose == null || previousClose.signum() <= 0) return null;
        return close.subtract(previousClose).divide(previousClose, new MathContext(16)).multiply(BigDecimal.valueOf(100));
    }
}
