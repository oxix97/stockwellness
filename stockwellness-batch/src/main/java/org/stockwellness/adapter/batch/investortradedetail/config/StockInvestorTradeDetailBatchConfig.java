package org.stockwellness.adapter.batch.investortradedetail.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stockwellness.batch.support.BatchMdcListener;

@Configuration
public class StockInvestorTradeDetailBatchConfig {

    @Bean
    public Job stockInvestorTradeDetailJob(
            @Qualifier("kisEodInvestorTradeStep") Step canonicalInvestorTradeStep,
            JobRepository jobRepository,
            BatchMdcListener mdcListener,
            JobExecutionListener commonJobListener
    ) {
        return new JobBuilder("stockInvestorTradeDetailJob", jobRepository)
                .start(canonicalInvestorTradeStep)
                .listener(mdcListener)
                .listener(commonJobListener)
                .build();
    }
}
