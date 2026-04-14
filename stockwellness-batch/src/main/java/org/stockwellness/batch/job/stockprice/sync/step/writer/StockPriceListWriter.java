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

import org.springframework.jdbc.core.JdbcTemplate;
import org.stockwellness.application.port.in.batch.StockPriceSyncUseCase;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.StockInvestorTrade;
import org.stockwellness.global.util.DateUtil;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class StockPriceListWriter implements ItemWriter<StockPriceSyncUseCase.StockPriceSyncResult> {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcBatchItemWriter<StockPrice> stockPriceJdbcWriter;

    @Override
    public void write(Chunk<? extends StockPriceSyncUseCase.StockPriceSyncResult> chunk) throws Exception {
        List<StockPrice> flatPrices = new ArrayList<>();
        List<StockInvestorTrade> flatInvestorTrades = new ArrayList<>();

        for (StockPriceSyncUseCase.StockPriceSyncResult result : chunk) {
            if (result != null) {
                flatPrices.addAll(result.stockPrices());
                flatInvestorTrades.addAll(result.investorTrades());
            }
        }

        if (flatPrices.isEmpty() && flatInvestorTrades.isEmpty()) {
            return;
        }

        if (!flatPrices.isEmpty()) {
            // stock_price 행을 재생성
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

        if (!flatInvestorTrades.isEmpty()) {
            saveInvestorTrades(flatInvestorTrades);
        }
    }

    private void saveInvestorTrades(List<StockInvestorTrade> trades) {
        String sql = """
                INSERT INTO stock_investor_trade (
                    stock_id, base_date, name, ticker,
                    frgn_ntby_qty, orgn_ntby_qty, ivtr_ntby_qty, bank_ntby_qty, insu_ntby_qty, mrbn_ntby_qty, fund_ntby_qty, etc_orgt_ntby_vol, etc_corp_ntby_vol,
                    frgn_ntby_tr_pbmn, orgn_ntby_tr_pbmn, ivtr_ntby_tr_pbmn, bank_ntby_tr_pbmn, insu_ntby_tr_pbmn, mrbn_ntby_tr_pbmn, fund_ntby_tr_pbmn, etc_orgt_ntby_tr_pbmn, etc_corp_ntby_tr_pbmn,
                    created_at
                ) VALUES (
                    CAST(? AS bigint), CAST(? AS date), CAST(? AS varchar), CAST(? AS varchar),
                    CAST(? AS bigint), CAST(? AS bigint), CAST(? AS bigint), CAST(? AS bigint), CAST(? AS bigint), CAST(? AS bigint), CAST(? AS bigint), CAST(? AS bigint), CAST(? AS bigint),
                    CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric), CAST(? AS numeric),
                    CURRENT_TIMESTAMP
                )
                ON CONFLICT (stock_id, base_date) DO UPDATE SET
                    name = EXCLUDED.name,
                    ticker = EXCLUDED.ticker,
                    frgn_ntby_qty = EXCLUDED.frgn_ntby_qty,
                    orgn_ntby_qty = EXCLUDED.orgn_ntby_qty,
                    ivtr_ntby_qty = EXCLUDED.ivtr_ntby_qty,
                    bank_ntby_qty = EXCLUDED.bank_ntby_qty,
                    insu_ntby_qty = EXCLUDED.insu_ntby_qty,
                    mrbn_ntby_qty = EXCLUDED.mrbn_ntby_qty,
                    fund_ntby_qty = EXCLUDED.fund_ntby_qty,
                    etc_orgt_ntby_vol = EXCLUDED.etc_orgt_ntby_vol,
                    etc_corp_ntby_vol = EXCLUDED.etc_corp_ntby_vol,
                    frgn_ntby_tr_pbmn = EXCLUDED.frgn_ntby_tr_pbmn,
                    orgn_ntby_tr_pbmn = EXCLUDED.orgn_ntby_tr_pbmn,
                    ivtr_ntby_tr_pbmn = EXCLUDED.ivtr_ntby_tr_pbmn,
                    bank_ntby_tr_pbmn = EXCLUDED.bank_ntby_tr_pbmn,
                    insu_ntby_tr_pbmn = EXCLUDED.insu_ntby_tr_pbmn,
                    mrbn_ntby_tr_pbmn = EXCLUDED.mrbn_ntby_tr_pbmn,
                    fund_ntby_tr_pbmn = EXCLUDED.fund_ntby_tr_pbmn,
                    etc_orgt_ntby_tr_pbmn = EXCLUDED.etc_orgt_ntby_tr_pbmn,
                    etc_corp_ntby_tr_pbmn = EXCLUDED.etc_corp_ntby_tr_pbmn
                """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                StockInvestorTrade trade = trades.get(i);
                int idx = 1;
                ps.setLong(idx++, trade.getId().getStockId());
                ps.setDate(idx++, DateUtil.toSqlDate(trade.getId().getBaseDate()));
                ps.setString(idx++, trade.getName());
                ps.setString(idx++, trade.getTicker());
                ps.setObject(idx++, trade.getFrgnNtbyQty());
                ps.setObject(idx++, trade.getOrgnNtbyQty());
                ps.setObject(idx++, trade.getIvtrNtbyQty());
                ps.setObject(idx++, trade.getBankNtbyQty());
                ps.setObject(idx++, trade.getInsuNtbyQty());
                ps.setObject(idx++, trade.getMrbnNtbyQty());
                ps.setObject(idx++, trade.getFundNtbyQty());
                ps.setObject(idx++, trade.getEtcOrgtNtbyVol());
                ps.setObject(idx++, trade.getEtcCorpNtbyVol());
                ps.setBigDecimal(idx++, trade.getFrgnNtbyTrPbmn());
                ps.setBigDecimal(idx++, trade.getOrgnNtbyTrPbmn());
                ps.setBigDecimal(idx++, trade.getIvtrNtbyTrPbmn());
                ps.setBigDecimal(idx++, trade.getBankNtbyTrPbmn());
                ps.setBigDecimal(idx++, trade.getInsuNtbyTrPbmn());
                ps.setBigDecimal(idx++, trade.getMrbnNtbyTrPbmn());
                ps.setBigDecimal(idx++, trade.getFundNtbyTrPbmn());
                ps.setBigDecimal(idx++, trade.getEtcOrgtNtbyTrPbmn());
                ps.setBigDecimal(idx++, trade.getEtcCorpNtbyTrPbmn());
            }

            @Override
            public int getBatchSize() {
                return trades.size();
            }
        });
    }
}
