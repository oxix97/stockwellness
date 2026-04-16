package org.stockwellness.adapter.batch.stock.step.processor;

import org.springframework.batch.item.ItemProcessor;
import org.stockwellness.application.port.in.batch.StockMasterSyncUseCase;
import org.stockwellness.domain.stock.KosdaqItem;
import org.stockwellness.domain.stock.KospiItem;
import org.stockwellness.domain.stock.Stock;

/**
 * KIS 마스터 아이템을 {@link Stock} 엔티티로 변환하는 Processor.
 *
 * <p>생성 시점에 주입받은 {@code marketIndexMap}을 사용하여
 * KIS 마스터의 대/중/소분류 코드를 기반으로 {@link StockSector}를 구성합니다.
 */
public class StockItemProcessor {

    private StockItemProcessor() {
    }

    // ── KOSPI ─────────────────────────────────────────────────────────────────

    public static class Kospi implements ItemProcessor<KospiItem, Stock> {

        private final StockMasterSyncUseCase stockMasterSyncUseCase;

        public Kospi(StockMasterSyncUseCase stockMasterSyncUseCase) {
            this.stockMasterSyncUseCase = stockMasterSyncUseCase;
        }

        @Override
        public Stock process(KospiItem item) {
            return stockMasterSyncUseCase.upsertKospi(new StockMasterSyncUseCase.KospiMasterSyncCommand(item)).stock();
        }
    }

    // ── KOSDAQ ────────────────────────────────────────────────────────────────

    public static class Kosdaq implements ItemProcessor<KosdaqItem, Stock> {

        private final StockMasterSyncUseCase stockMasterSyncUseCase;

        public Kosdaq(StockMasterSyncUseCase stockMasterSyncUseCase) {
            this.stockMasterSyncUseCase = stockMasterSyncUseCase;
        }

        @Override
        public Stock process(KosdaqItem item) {
            return stockMasterSyncUseCase.upsertKosdaq(new StockMasterSyncUseCase.KosdaqMasterSyncCommand(item)).stock();
        }
    }
}
