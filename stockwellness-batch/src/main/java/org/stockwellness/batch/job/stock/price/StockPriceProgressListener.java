package org.stockwellness.batch.job.stock.price;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.stockwellness.domain.stock.StockPrice;

@Slf4j
@Component
public class StockPriceProgressListener implements ItemWriteListener<List<StockPrice>> {

    private final AtomicInteger count = new AtomicInteger(0);

    @Override
    public void afterWrite(Chunk<? extends List<StockPrice>> items) {
        int currentCount = count.incrementAndGet();
        // 10개 종목마다 로그 출력
        if (currentCount % 10 == 0) {
            log.info(">>> [Batch Progress] {} stocks processed successfully.", currentCount);
        }
    }

    @Override
    public void onWriteError(Exception exception, Chunk<? extends List<StockPrice>> items) {
        log.error(">>> [Batch Error] Error occurred while writing stock prices", exception);
    }
}
