package org.stockwellness.batch.job.stock.price.repair;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.global.util.QueryTypeUtil;
import org.stockwellness.global.util.DateUtil;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockPriceRepairBatchConfig {
import org.stockwellness.batch.common.BatchMdcListener;
import org.stockwellness.domain.stock.Stock;
...
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;
    private final BatchMdcListener mdcListener;

    @Bean
    public Job stockPricePrevCloseSyncJob(Step stockPricePrevCloseStep) {
        return new JobBuilder("stockPricePrevCloseSyncJob", jobRepository)
                .start(stockPricePrevCloseStep)
                .listener(mdcListener)
                .build();
    }

    @Bean
    public Step stockPricePrevCloseStep(
            ItemReader<Stock> stockRepairReader,
            ItemProcessor<Stock, List<StockPriceRepairDto>> stockPricePrevCloseProcessor,
            ItemWriter<List<StockPriceRepairDto>> stockPriceSimpleRepairWriter
    ) {
        return new StepBuilder("stockPricePrevCloseStep", jobRepository)
                .<Stock, List<StockPriceRepairDto>>chunk(1, transactionManager) 
                .reader(stockRepairReader)
                .processor(stockPricePrevCloseProcessor)
                .writer(stockPriceSimpleRepairWriter)
                .listener(mdcListener)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Stock> stockRepairReader(
            @Value("#{jobParameters['targetTicker']}") String targetTicker
    ) {
        String query = "SELECT s FROM Stock s WHERE s.status = 'ACTIVE'";
        if (targetTicker != null && !targetTicker.trim().isEmpty() && !targetTicker.equalsIgnoreCase("null")) {
            query += " AND s.ticker = '" + targetTicker + "'";
        }
        query += " ORDER BY s.id ASC";

        return new JpaPagingItemReaderBuilder<Stock>()
                .name("stockRepairReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(query)
                .pageSize(10)
                .saveState(false)
                .build();
    }

    @Bean
    public ItemWriter<List<StockPriceRepairDto>> stockPriceSimpleRepairWriter(
            JdbcBatchItemWriter<StockPriceRepairDto> stockPriceOnlyPrevCloseUpdateWriter
    ) {
        return chunk -> {
            List<StockPriceRepairDto> flatList = new ArrayList<>();
            for (List<StockPriceRepairDto> list : chunk) {
                if (list != null) flatList.addAll(list);
            }
            if (!flatList.isEmpty()) {
                stockPriceOnlyPrevCloseUpdateWriter.write(new Chunk<>(flatList));
            }
        };
    }

    @Bean
    public JdbcBatchItemWriter<StockPriceRepairDto> stockPriceOnlyPrevCloseUpdateWriter() {
        // [중요] WHERE 절에 명시적 타입 캐스팅을 적용하여 DKME 등 날짜 매칭 오류를 해결
        String sql = String.format(
                "UPDATE stock_price SET prev_close_price = %s WHERE stock_id = %s AND base_date = %s",
                QueryTypeUtil.NUMERIC, 
                QueryTypeUtil.BIGINT,  
                QueryTypeUtil.DATE      
        );

        return new JdbcBatchItemWriterBuilder<StockPriceRepairDto>()
                .dataSource(dataSource)
                .sql(sql)
                .itemPreparedStatementSetter((item, ps) -> {
                    ps.setBigDecimal(1, item.calculatedPrevClose());
                    ps.setLong(2, item.stockId());
                    ps.setDate(3, DateUtil.toSqlDate(item.baseDate()));
                })
                .assertUpdates(false) // 0건이어도 예외를 발생시키지 않음 (정상적인 Skip 대응)
                .build();
    }
}
