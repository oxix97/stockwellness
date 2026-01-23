package org.stockwellness.adapter.in.batch.step;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.domain.stock.StockHistory;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class StockIndicatorCalculate {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final StockRepository stockRepository;
    private final StockHistoryProcessor processor;
    private final StockHistoryWriter writer;

    @Bean
    public Step calculateStock(@Qualifier("batchExecutor") TaskExecutor taskExecutor) {
        return new StepBuilder("calculateStock", jobRepository)
                .<String, List<StockHistory>>chunk(5, transactionManager)
                .reader(isinCodeReader())
                .processor(processor)
                .writer(writer)
                .taskExecutor(taskExecutor) // 병렬 처리 활성화
                .throttleLimit(10)          // 최대 동시 실행 스레드 수 제한
                .build();
    }

    @Bean
    public ListItemReader<String> isinCodeReader() {
        return new ListItemReader<>(stockRepository.findAllByIsinCode());
    }

}
