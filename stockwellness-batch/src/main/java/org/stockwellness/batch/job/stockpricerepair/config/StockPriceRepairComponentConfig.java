package org.stockwellness.batch.job.stockpricerepair.config;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stockwellness.batch.job.stockpricerepair.model.StockPriceRepairDto;
import org.stockwellness.batch.job.stockpricerepair.support.StockPriceRepairSql;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.global.util.DateUtil;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class StockPriceRepairComponentConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;

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
        return new JdbcBatchItemWriterBuilder<StockPriceRepairDto>()
                .dataSource(dataSource)
                .sql(StockPriceRepairSql.UPDATE_PREV_CLOSE)
                .itemPreparedStatementSetter((item, ps) -> {
                    ps.setBigDecimal(1, item.calculatedPrevClose());
                    ps.setLong(2, item.stockId());
                    ps.setDate(3, DateUtil.toSqlDate(item.baseDate()));
                })
                .assertUpdates(false)
                .build();
    }
}
