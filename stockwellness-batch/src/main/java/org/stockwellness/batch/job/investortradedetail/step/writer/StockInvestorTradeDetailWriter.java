package org.stockwellness.batch.job.investortradedetail.step.writer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.stockwellness.batch.job.investortradedetail.model.InvestorTradeDetailUpdateCommand;
import org.stockwellness.global.util.DateUtil;
import org.stockwellness.global.util.QueryTypeUtil;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Slf4j
public class StockInvestorTradeDetailWriter implements ItemWriter<InvestorTradeDetailUpdateCommand> {

    private final JdbcTemplate jdbcTemplate;

    public StockInvestorTradeDetailWriter(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    StockInvestorTradeDetailWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void write(Chunk<? extends InvestorTradeDetailUpdateCommand> chunk) {
        List<? extends InvestorTradeDetailUpdateCommand> items = chunk.getItems();
        if (items.isEmpty()) {
            return;
        }

        String sql = String.format(
                """
                UPDATE stock_price SET
                    inst_buying_amt = %s,
                    frgn_buying_amt = %s,
                    total_net_amt = %s,
                    inst_buying_qty = %s,
                    frgn_buying_qty = %s,
                    total_net_qty = %s
                WHERE stock_id = %s AND base_date = %s
                """,
                QueryTypeUtil.NUMERIC, QueryTypeUtil.NUMERIC, QueryTypeUtil.NUMERIC,
                QueryTypeUtil.BIGINT, QueryTypeUtil.BIGINT, QueryTypeUtil.BIGINT,
                QueryTypeUtil.BIGINT,
                QueryTypeUtil.DATE
        );

        int[] updated = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                InvestorTradeDetailUpdateCommand item = items.get(i);
                var sd = item.supplyDemand();
                int idx = 1;
                ps.setBigDecimal(idx++, sd.getInstitutionAmt());
                ps.setBigDecimal(idx++, sd.getForeignAmt());
                ps.setBigDecimal(idx++, sd.getTotalNetAmt());
                
                ps.setLong(idx++, sd.getInstitutionQty());
                ps.setLong(idx++, sd.getForeignQty());
                ps.setLong(idx++, sd.getTotalNetQty());

                ps.setLong(idx++, item.stockId());
                ps.setDate(idx++, DateUtil.toSqlDate(item.baseDate()));
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });

        for (int i = 0; i < updated.length; i++) {
            if (updated[i] == 0) {
                InvestorTradeDetailUpdateCommand item = items.get(i);
                log.info(
                        "[투자주체 수급 보정 Writer] 대상 stock_price 행이 없어 업데이트를 건너뜁니다. stockId={}, baseDate={}",
                        item.stockId(),
                        item.baseDate()
                );
            }
        }

        jdbcTemplate.batchUpdate(
                "DELETE FROM stock_investor_trade WHERE stock_id = CAST(? AS bigint) AND base_date = CAST(? AS date)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        InvestorTradeDetailUpdateCommand item = items.get(i);
                        ps.setLong(1, item.stockId());
                        ps.setDate(2, DateUtil.toSqlDate(item.baseDate()));
                    }

                    @Override
                    public int getBatchSize() {
                        return items.size();
                    }
                }
        );

        String investorTradeInsertSql = String.format(
                """
                INSERT INTO stock_investor_trade (
                    base_date, stock_id,
                    inst_buying_amt, frgn_buying_amt, pension_buying_amt, trust_buying_amt, etc_corp_buying_amt, total_net_amt,
                    inst_buying_qty, frgn_buying_qty, pension_buying_qty, trust_buying_qty, etc_corp_buying_qty, total_net_qty,
                    created_at
                ) VALUES (
                    %s, %s,
                    %s, %s, %s, %s, %s, %s,
                    %s, %s, %s, %s, %s, %s,
                    CURRENT_TIMESTAMP
                )
                """,
                QueryTypeUtil.DATE, QueryTypeUtil.BIGINT,
                QueryTypeUtil.NUMERIC, QueryTypeUtil.NUMERIC, QueryTypeUtil.NUMERIC, QueryTypeUtil.NUMERIC, QueryTypeUtil.NUMERIC, QueryTypeUtil.NUMERIC,
                QueryTypeUtil.BIGINT, QueryTypeUtil.BIGINT, QueryTypeUtil.BIGINT, QueryTypeUtil.BIGINT, QueryTypeUtil.BIGINT, QueryTypeUtil.BIGINT
        );

        jdbcTemplate.batchUpdate(investorTradeInsertSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                InvestorTradeDetailUpdateCommand item = items.get(i);
                var sd = item.supplyDemand();
                int idx = 1;
                ps.setDate(idx++, DateUtil.toSqlDate(item.baseDate()));
                ps.setLong(idx++, item.stockId());
                ps.setBigDecimal(idx++, sd.getInstitutionAmt());
                ps.setBigDecimal(idx++, sd.getForeignAmt());
                ps.setBigDecimal(idx++, sd.getPensionFundAmt());
                ps.setBigDecimal(idx++, sd.getTrustAmt());
                ps.setBigDecimal(idx++, sd.getEtcCorpAmt());
                ps.setBigDecimal(idx++, sd.getTotalNetAmt());
                ps.setLong(idx++, sd.getInstitutionQty());
                ps.setLong(idx++, sd.getForeignQty());
                ps.setLong(idx++, sd.getPensionFundQty());
                ps.setLong(idx++, sd.getTrustQty());
                ps.setLong(idx++, sd.getEtcCorpQty());
                ps.setLong(idx++, sd.getTotalNetQty());
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });
    }
}
