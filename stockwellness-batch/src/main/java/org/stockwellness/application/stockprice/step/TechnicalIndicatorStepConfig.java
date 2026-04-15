package org.stockwellness.application.stockprice.step;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.application.stockprice.step.processor.TechnicalIndicatorProcessor;
import org.stockwellness.application.stockprice.step.writer.StockPriceWriter;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

/**
 * [Step 2-2] 기술 지표 계산
 */
@Configuration
public class TechnicalIndicatorStepConfig {

    @Bean
    public Step technicalIndicatorCalculateStep(
            JobRepository jobRepository,
            PlatformTransactionManager txManager,
            JpaPagingItemReader<Stock> technicalIndicatorReader,
            TechnicalIndicatorProcessor technicalIndicatorProcessor,
            StockPriceWriter technicalIndicatorWriter,
            TaskExecutor batchExecutor
    ) {
        return new StepBuilder("technicalIndicatorCalculateStep", jobRepository)
                .<Stock, StockPrice>chunk(300, txManager)
                .reader(technicalIndicatorReader)
                .processor(technicalIndicatorProcessor)
                .writer(technicalIndicatorWriter)
                .taskExecutor(batchExecutor)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Stock> technicalIndicatorReader(EntityManagerFactory emf) {
        JpaPagingItemReader<Stock> reader = new JpaPagingItemReader<>();
        reader.setEntityManagerFactory(emf);
        reader.setPageSize(300);
        reader.setQueryString("""
                select s
                from Stock s
                where s.status = org.stockwellness.domain.stock.StockStatus.ACTIVE
                order by s.id asc
                """);
        reader.setSaveState(false);
        return reader;
    }

    @Bean
    @StepScope
    public TechnicalIndicatorProcessor technicalIndicatorProcessor(StockPricePort stockPricePort) {
        return new TechnicalIndicatorProcessor(stockPricePort);
    }

    @Bean
    public StockPriceWriter technicalIndicatorWriter(StockPricePort stockPricePort) {
        return new StockPriceWriter(stockPricePort);
    }
}
