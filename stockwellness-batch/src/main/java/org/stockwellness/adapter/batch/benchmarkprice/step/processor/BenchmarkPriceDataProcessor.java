package org.stockwellness.adapter.batch.benchmarkprice.step.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.stockwellness.adapter.batch.benchmarkprice.model.BenchmarkPriceDataWrapper;
import org.stockwellness.application.port.in.batch.BenchmarkPriceSyncUseCase;
import org.stockwellness.domain.stock.price.BenchmarkPrice;

/**
 * [Processor] 수집된 시세 데이터를 BenchmarkPrice 엔티티로 변환하고 필요한 등락률을 계산하는 클래스
 */
@RequiredArgsConstructor
public class BenchmarkPriceDataProcessor implements ItemProcessor<BenchmarkPriceDataWrapper, BenchmarkPrice> {

    private final BenchmarkPriceSyncUseCase benchmarkPriceSyncUseCase;

    /**
     * 개별 시세 데이터를 처리하여 엔티티로 변환합니다.
     */
    @Override
    public BenchmarkPrice process(BenchmarkPriceDataWrapper item) {
        return benchmarkPriceSyncUseCase.toBenchmarkPrice(
                new BenchmarkPriceSyncUseCase.BenchmarkPriceCommand(
                        item.type(),
                        item.data().baseDate(),
                        item.data().openPrice(),
                        item.data().highPrice(),
                        item.data().lowPrice(),
                        item.data().closePrice(),
                        item.data().prdyCtrt(),
                        item.data().volume()
                )
        ).benchmarkPrice();
    }
}
