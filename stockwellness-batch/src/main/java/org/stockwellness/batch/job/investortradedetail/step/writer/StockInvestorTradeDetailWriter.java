package org.stockwellness.batch.job.investortradedetail.step.writer;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.stockwellness.batch.job.investortradedetail.model.InvestorTradeDetailUpdateCommand;
import org.stockwellness.global.util.DateUtil;

import java.sql.Types;
import java.util.List;

@RequiredArgsConstructor
public class StockInvestorTradeDetailWriter implements ItemWriter<InvestorTradeDetailUpdateCommand> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void write(Chunk<? extends InvestorTradeDetailUpdateCommand> chunk) {
        List<? extends InvestorTradeDetailUpdateCommand> items = chunk.getItems();
        if (items.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(upsertSql(), new BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                InvestorTradeDetailUpdateCommand item = items.get(i);
                int idx = 1;
                ps.setLong(idx++, item.stockId());
                ps.setDate(idx++, DateUtil.toSqlDate(item.baseDate()));
                ps.setString(idx++, item.name());
                ps.setString(idx++, item.ticker());
                ps.setLong(idx++, item.frgnNtbyQty());
                ps.setLong(idx++, item.orgnNtbyQty());
                ps.setLong(idx++, item.ivtrNtbyQty());
                ps.setLong(idx++, item.bankNtbyQty());
                ps.setLong(idx++, item.insuNtbyQty());
                ps.setLong(idx++, item.mrbnNtbyQty());
                ps.setLong(idx++, item.fundNtbyQty());
                ps.setLong(idx++, item.etcOrgtNtbyVol());
                ps.setLong(idx++, item.etcCorpNtbyVol());
                ps.setBigDecimal(idx++, item.frgnNtbyTrPbmn());
                ps.setBigDecimal(idx++, item.orgnNtbyTrPbmn());
                ps.setBigDecimal(idx++, item.ivtrNtbyTrPbmn());
                ps.setBigDecimal(idx++, item.bankNtbyTrPbmn());
                ps.setBigDecimal(idx++, item.insuNtbyTrPbmn());
                ps.setBigDecimal(idx++, item.mrbnNtbyTrPbmn());
                ps.setBigDecimal(idx++, item.fundNtbyTrPbmn());
                ps.setBigDecimal(idx++, item.etcOrgtNtbyTrPbmn());
                ps.setBigDecimal(idx++, item.etcCorpNtbyTrPbmn());
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });
    }

    private String upsertSql() {
        return """
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
    }
}
