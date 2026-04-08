package org.stockwellness.batch.job.stockprice.sync.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.item.Chunk;
import org.stockwellness.adapter.out.kafka.batch.KafkaEventPublisher;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class StockPriceSyncEventListenerTest {

    @Test
    @DisplayName("멀티스레드 환경에서 업데이트된 종목 코드가 누락 없이 수집되어야 한다")
    void concurrencyTest() throws InterruptedException {
        // given
        KafkaEventPublisher kafkaEventPublisher = Mockito.mock(KafkaEventPublisher.class);
        StockPriceSyncEventListener listener = new StockPriceSyncEventListener(kafkaEventPublisher);
        
        int threadCount = 10;
        int itemsPerThread = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    List<StockPrice> stockPrices = new ArrayList<>();
                    for (int j = 0; j < itemsPerThread; j++) {
                        String ticker = String.format("%06d", threadId * itemsPerThread + j);
                        Stock stock = Mockito.mock(Stock.class);
                        Mockito.when(stock.getTicker()).thenReturn(ticker);
                        
                        StockPrice stockPrice = Mockito.mock(StockPrice.class);
                        Mockito.when(stockPrice.getStock()).thenReturn(stock);
                        
                        stockPrices.add(stockPrice);
                    }
                    
                    // Chunk<List<StockPrice>> 형태로 afterWrite 호출
                    listener.afterWrite(new Chunk<>(Collections.singletonList(stockPrices)));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        // 총 종목 수 = threadCount * itemsPerThread
        // StockPriceSyncEventListener 내부의 updatedSymbols 접근은 리플렉션이나 afterJob을 통해 검증 가능
        // 하지만 여기서는 public API인 afterJob 호출 시점에 kafkaEventPublisher에 전달되는 리스트 크기로 검증
        
        org.springframework.batch.core.JobExecution jobExecution = Mockito.mock(org.springframework.batch.core.JobExecution.class);
        org.springframework.batch.core.JobParameters jobParameters = new org.springframework.batch.core.JobParametersBuilder()
                .addString("publishEvent", "true")
                .toJobParameters();
        
        Mockito.when(jobExecution.getJobParameters()).thenReturn(jobParameters);
        Mockito.when(jobExecution.getStatus()).thenReturn(org.springframework.batch.core.BatchStatus.COMPLETED);

        listener.afterJob(jobExecution);

        // then: kafkaEventPublisher.publishStockPriceUpdated 가 정확히 1번 호출되고 리스트 사이즈가 예상과 같아야 함
        Mockito.verify(kafkaEventPublisher).publishStockPriceUpdated(Mockito.argThat(list -> {
            assertThat(list).hasSize(threadCount * itemsPerThread);
            return true;
        }));
    }
}
