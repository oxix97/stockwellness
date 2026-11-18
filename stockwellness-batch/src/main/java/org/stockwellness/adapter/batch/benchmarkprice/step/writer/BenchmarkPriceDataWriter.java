package org.stockwellness.adapter.batch.benchmarkprice.step.writer;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.stockwellness.application.port.out.stock.BenchmarkPricePort;
import org.stockwellness.domain.stock.price.BenchmarkPrice;

/**
 * [Writer] 변환된 BenchmarkPrice 엔티티를 DB에 저장하거나 업데이트하는 클래스
 */
@RequiredArgsConstructor
public class BenchmarkPriceDataWriter implements ItemWriter<BenchmarkPrice> {

    private final BenchmarkPricePort benchmarkPricePort;

    /**
     * Chunk 단위로 들어오는 데이터를 일괄 처리(Upsert)합니다.
     */
    @Override
    public void write(Chunk<? extends BenchmarkPrice> chunk) {
        for (BenchmarkPrice bp : chunk) {
            benchmarkPricePort.save(bp);
        }
    }
}
