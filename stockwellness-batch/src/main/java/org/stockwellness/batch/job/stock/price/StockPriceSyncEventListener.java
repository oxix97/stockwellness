package org.stockwellness.batch.job.stock.price;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;
import org.stockwellness.batch.event.KafkaEventPublisher;
import org.stockwellness.domain.stock.price.StockPrice;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@JobScope
@RequiredArgsConstructor
public class StockPriceSyncEventListener implements ItemWriteListener<List<StockPrice>>, JobExecutionListener {

    private final KafkaEventPublisher kafkaEventPublisher;
    private final Set<String> updatedSymbols = ConcurrentHashMap.newKeySet();

    @Override
    public void afterWrite(Chunk<? extends List<StockPrice>> items) {
        for (List<StockPrice> stockPrices : items) {
            if (stockPrices == null) continue;
            for (StockPrice stockPrice : stockPrices) {
                if (stockPrice != null && stockPrice.getStock() != null) {
                    updatedSymbols.add(stockPrice.getStock().getTicker());
                }
            }
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        // [수정] JobParameter에서 publishEvent 값을 확인하여 조건부 발행
        String publishEvent = jobExecution.getJobParameters().getString("publishEvent", "false");
        
        if (!"true".equalsIgnoreCase(publishEvent)) {
            log.info(">>> Kafka event publishing is DISABLED for this job execution (publishEvent=false).");
            updatedSymbols.clear();
            return;
        }

        if (jobExecution.getStatus().isUnsuccessful()) {
            log.warn("Stock price sync job failed. Skipping event publishing.");
            updatedSymbols.clear();
            return;
        }

        if (!updatedSymbols.isEmpty()) {
            List<String> symbols = new ArrayList<>(updatedSymbols);
            log.info("Stock price sync job completed. Publishing events for {} symbols.", symbols.size());
            kafkaEventPublisher.publishStockPriceUpdated(symbols);
            updatedSymbols.clear();
        } else {
            log.info("Stock price sync job completed, but no symbols were updated.");
        }
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        updatedSymbols.clear();
    }
}
