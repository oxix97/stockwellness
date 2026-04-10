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
                UPDATE stock_price
                   SET net_institutional_buying_amt = %s,
                       net_foreign_buying_amt = %s,
                       net_total_buying_amt = %s,
                       net_institutional_buying_qty = %s,
                       net_foreign_buying_qty = %s
                 WHERE stock_id = %s
                   AND base_date = %s
                """,
                QueryTypeUtil.NUMERIC,
                QueryTypeUtil.NUMERIC,
                QueryTypeUtil.NUMERIC,
                QueryTypeUtil.BIGINT,
                QueryTypeUtil.BIGINT,
                QueryTypeUtil.BIGINT,
                QueryTypeUtil.DATE
        );

        int[] updated = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                InvestorTradeDetailUpdateCommand item = items.get(i);
                ps.setBigDecimal(1, item.netInstitutionalBuyingAmt());
                ps.setBigDecimal(2, item.netForeignBuyingAmt());
                ps.setBigDecimal(3, item.netInstitutionalBuyingAmt().add(item.netForeignBuyingAmt()));
                ps.setLong(4, item.netInstitutionalBuyingQty());
                ps.setLong(5, item.netForeignBuyingQty());
                ps.setLong(6, item.stockId());
                ps.setDate(7, DateUtil.toSqlDate(item.baseDate()));
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
    }
}
