package org.stockwellness.batch.job.investortradedetail.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.batch.job.investortradedetail.application.StockInvestorTradeDetailBatchService;
import org.stockwellness.batch.job.investortradedetail.step.processor.StockInvestorTradeDetailProcessor;
import org.stockwellness.batch.job.investortradedetail.step.reader.StockInvestorTradeDetailReader;
import org.stockwellness.batch.job.investortradedetail.step.writer.StockInvestorTradeDetailWriter;

import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
public class StockInvestorTradeDetailComponentConfig {

    private final StockInvestorTradeDetailBatchService batchService;
    private final StockRepository stockRepository;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    @StepScope
    public StockInvestorTradeDetailReader stockInvestorTradeDetailReader() {
        return new StockInvestorTradeDetailReader(batchService.fetchMergedDetails());
    }

    @Bean
    @StepScope
    public StockInvestorTradeDetailProcessor stockInvestorTradeDetailProcessor() {
        LocalDate marketBaseDate = batchService.resolveMarketBaseDate();
        return new StockInvestorTradeDetailProcessor(stockRepository, marketBaseDate);
    }

    @Bean
    public StockInvestorTradeDetailWriter stockInvestorTradeDetailWriter() {
        return new StockInvestorTradeDetailWriter(jdbcTemplate);
    }
}
