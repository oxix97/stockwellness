package org.stockwellness.batch.job.stock.master;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.domain.stock.KosdaqItem;
import org.stockwellness.domain.stock.KospiItem;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockSector;
import org.stockwellness.domain.stock.insight.MarketIndex;

import java.util.Map;

/**
 * KIS 마스터 아이템을 {@link Stock} 엔티티로 변환하는 Processor.
 *
 * <p>생성 시점에 주입받은 {@code marketIndexMap}을 사용하여
 * KIS 마스터의 대/중/소분류 코드를 기반으로 {@link StockSector}를 구성합니다.
 */
@Slf4j
public class StockItemProcessor {

    private StockItemProcessor() {
    }

    // ── KOSPI ─────────────────────────────────────────────────────────────────

    public static class Kospi implements ItemProcessor<KospiItem, Stock> {

        private final StockRepository stockRepository;
        private final Map<String, MarketIndex> indexMap;

        public Kospi(StockRepository stockRepository, Map<String, MarketIndex> indexMap) {
            this.stockRepository = stockRepository;
            this.indexMap = indexMap;
        }

        @Override
        public Stock process(KospiItem item) {
            if (item.shortCode() == null || item.shortCode().isBlank()) {
                log.warn("[KOSPI] 단축코드 없음, skip: isin={}", item.isinCode());
                return null;
            }

            // 업종 정보 생성 (소 > 중 > 대 우선순위 매핑)
            StockSector sector = StockSector.of(
                    item.sectorLarge(),
                    item.sectorMedium(),
                    item.sectorSmall(),
                    indexMap
            );

            return stockRepository.findByTicker(item.shortCode())
                    .map(existing -> {
                        existing.updateFromKospi(item, sector);
                        return existing;
                    })
                    .orElseGet(() -> Stock.ofKospi(item, sector));
        }
    }

    // ── KOSDAQ ────────────────────────────────────────────────────────────────

    public static class Kosdaq implements ItemProcessor<KosdaqItem, Stock> {

        private final StockRepository stockRepository;
        private final Map<String, MarketIndex> indexMap;

        public Kosdaq(StockRepository stockRepository, Map<String, MarketIndex> indexMap) {
            this.stockRepository = stockRepository;
            this.indexMap = indexMap;
        }

        @Override
        public Stock process(KosdaqItem item) {
            if (item.shortCode() == null || item.shortCode().isBlank()) {
                log.warn("[KOSDAQ] 단축코드 없음, skip: isin={}", item.isinCode());
                return null;
            }

            // 업종 정보 생성 (소 > 중 > 대 우선순위 매핑)
            StockSector sector = StockSector.of(
                    item.sectorLarge(),
                    item.sectorMedium(),
                    item.sectorSmall(),
                    indexMap
            );

            return stockRepository.findByTicker(item.shortCode())
                    .map(existing -> {
                        existing.updateFromKosdaq(item, sector);
                        return existing;
                    })
                    .orElseGet(() -> Stock.ofKosdaq(item, sector));
        }
    }
}
