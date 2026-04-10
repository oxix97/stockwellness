package org.stockwellness.batch.job.investortradedetail.step.writer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.stockwellness.batch.job.investortradedetail.model.InvestorTradeDetailUpdateCommand;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockInvestorTradeDetailWriterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("stock_price 업데이트 배치를 실행하고 0건 업데이트도 예외 없이 처리한다")
    void write_updatesRowsAndAllowsZeroUpdatedRows() throws Exception {
        given(jdbcTemplate.batchUpdate(ArgumentMatchers.anyString(), ArgumentMatchers.any(BatchPreparedStatementSetter.class)))
                .willReturn(new int[]{1, 0});

        StockInvestorTradeDetailWriter writer = new StockInvestorTradeDetailWriter(jdbcTemplate);

        writer.write(new Chunk<>(
                new InvestorTradeDetailUpdateCommand(1L, LocalDate.of(2026, 4, 8), 10L, 20L, new BigDecimal("100"), new BigDecimal("200")),
                new InvestorTradeDetailUpdateCommand(2L, LocalDate.of(2026, 4, 8), 30L, 40L, new BigDecimal("300"), new BigDecimal("400"))
        ));

        verify(jdbcTemplate).batchUpdate(ArgumentMatchers.contains("UPDATE stock_price"), ArgumentMatchers.any(BatchPreparedStatementSetter.class));
        verify(jdbcTemplate).batchUpdate(ArgumentMatchers.contains("DELETE FROM stock_investor_trade"), ArgumentMatchers.any(BatchPreparedStatementSetter.class));
        verify(jdbcTemplate).batchUpdate(ArgumentMatchers.contains("INSERT INTO stock_investor_trade"), ArgumentMatchers.any(BatchPreparedStatementSetter.class));
        verify(jdbcTemplate, times(3)).batchUpdate(ArgumentMatchers.anyString(), ArgumentMatchers.any(BatchPreparedStatementSetter.class));
    }
}
