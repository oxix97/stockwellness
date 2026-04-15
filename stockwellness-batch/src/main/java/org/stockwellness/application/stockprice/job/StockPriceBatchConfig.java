package org.stockwellness.application.stockprice.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stockwellness.application.stockprice.listener.StockPriceSyncEventListener;

@Configuration
@RequiredArgsConstructor
public class StockPriceBatchConfig {

    private final JobRepository jobRepository;
    private final JobExecutionListener commonJobListener;
    private final StockPriceSyncEventListener eventListener;

    /**
     * 가격 정보 반영
     * @param dailyStockPriceStep: 멀티종목 시세조회를 통해 가격 정보를 저장한다.
     * @param technicalIndicatorCalculateStep: 기술 지표 계산.
     * @return
     */
    @Bean
    public Job dailyStockPriceBatchJob(
            Step dailyStockPriceStep,
            Step technicalIndicatorCalculateStep
    ) {
        return new JobBuilder("dailyStockPriceBatchJob", jobRepository)
                .start(dailyStockPriceStep)
                .next(technicalIndicatorCalculateStep)
                .listener(commonJobListener)
                .listener(eventListener)
                .build();
    }
}
