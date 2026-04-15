package org.stockwellness.application.investortradedetail.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.external.kis.dto.InvestorTradeDetail;
import org.stockwellness.application.investortradedetail.model.InvestorTradeDetailUpdateCommand;
import org.stockwellness.application.investortradedetail.step.processor.StockInvestorTradeDetailProcessor;
import org.stockwellness.application.investortradedetail.step.reader.StockInvestorTradeDetailReader;
import org.stockwellness.application.investortradedetail.step.writer.StockInvestorTradeDetailWriter;
import org.stockwellness.batch.support.BatchMdcListener;

@Configuration
@RequiredArgsConstructor
public class StockInvestorTradeDetailStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BatchMdcListener mdcListener;

    @Bean
    public Step stockInvestorTradeDetailStep(
            StockInvestorTradeDetailReader stockInvestorTradeDetailReader,
            StockInvestorTradeDetailProcessor stockInvestorTradeDetailProcessor,
            StockInvestorTradeDetailWriter stockInvestorTradeDetailWriter
    ) {
        return new StepBuilder("stockInvestorTradeDetailStep", jobRepository)
                .<InvestorTradeDetail, InvestorTradeDetailUpdateCommand>chunk(50, transactionManager)
                .reader(stockInvestorTradeDetailReader)
                .processor(stockInvestorTradeDetailProcessor)
                .writer(stockInvestorTradeDetailWriter)
                .listener(mdcListener)
                .build();
    }
}
