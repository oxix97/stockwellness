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
import org.stockwellness.adapter.batch.investortradedetail.model.InvestorTradeDetailUpdateCommand;
import org.stockwellness.adapter.batch.investortradedetail.step.writer.StockInvestorTradeDetailWriter;

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
    @DisplayName("stock_investor_trade upsert 배치를 실행한다")
    void write_upsertsRows() throws Exception {
        given(jdbcTemplate.batchUpdate(ArgumentMatchers.anyString(), ArgumentMatchers.any(BatchPreparedStatementSetter.class)))
                .willReturn(new int[]{1, 1});

        StockInvestorTradeDetailWriter writer = new StockInvestorTradeDetailWriter(jdbcTemplate);

        writer.write(new Chunk<>(
                command(1L, "005930"),
                command(2L, "000660")
        ));

        verify(jdbcTemplate).batchUpdate(ArgumentMatchers.contains("INSERT INTO stock_investor_trade"), ArgumentMatchers.any(BatchPreparedStatementSetter.class));
        verify(jdbcTemplate, times(1)).batchUpdate(ArgumentMatchers.anyString(), ArgumentMatchers.any(BatchPreparedStatementSetter.class));
    }

    private InvestorTradeDetailUpdateCommand command(Long stockId, String ticker) {
        return new InvestorTradeDetailUpdateCommand(
                stockId,
                LocalDate.of(2026, 4, 8),
                "종목" + ticker,
                ticker,
                10L,
                20L,
                30L,
                40L,
                50L,
                60L,
                70L,
                80L,
                90L,
                new BigDecimal("100"),
                new BigDecimal("200"),
                new BigDecimal("300"),
                new BigDecimal("400"),
                new BigDecimal("500"),
                new BigDecimal("600"),
                new BigDecimal("700"),
                new BigDecimal("800"),
                new BigDecimal("900")
        );
    }
}
