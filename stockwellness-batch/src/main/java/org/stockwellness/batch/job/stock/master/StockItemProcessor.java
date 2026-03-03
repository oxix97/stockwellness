package org.stockwellness.batch.job.stock.master;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.domain.stock.KosdaqItem;
import org.stockwellness.domain.stock.KospiItem;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.insight.MarketIndex;

/**
 * KIS 마스터 아이템을 {@link Stock} 엔티티로 변환하는 Processor.
 *
 * <p>생성 시점에 주입받은 {@code marketIndexMap}을 사용하여
 * KIS 마스터의 지수업종 중분류 코드(sectorMedium)를 {@link MarketIndex}와 매핑합니다.
 *
 * <pre>
 * 매핑 결과 → Stock 필드
 *   sectorCode = MarketCategory.name()  (e.g. "KOSPI_COMPOSITE")
 *   sectorName = MarketIndex.indexName  (e.g. "제약")
 *
 * 매핑 실패 시 (MarketIndex에 없는 코드)
 *   sectorCode = MarketCategory.UNKNOWN
 *   sectorName = null
 * </pre>
 *
 * <p>{@code marketIndexMap}은 Job 시작 시 한 번만 로딩되어
 * 모든 Chunk에서 공유됩니다 (DB 재조회 없음).
 */
@Slf4j
public class StockItemProcessor {

    private StockItemProcessor() {
    }

    // ── KOSPI ─────────────────────────────────────────────────────────────────

    public static class Kospi implements ItemProcessor<KospiItem, Stock> {

        private final StockRepository stockRepository;

        public Kospi(StockRepository stockRepository) {
            this.stockRepository = stockRepository;
        }

        @Override
        public Stock process(KospiItem item) {
            if (item.shortCode() == null || item.shortCode().isBlank()) {
                log.warn("[KOSPI] 단축코드 없음, skip: isin={}", item.isinCode());
                return null;
            }

            SectorInfo sector = resolveSector(item.sectorLarge(), item.sectorMedium(), item.sectorSmall(), item.koreanName());

            return stockRepository.findByTicker(item.shortCode())
                    .map(existing -> {
                        existing.updateFromKospi(item, sector.large(), sector.medium(), sector.small());
                        return existing;
                    })
                    .orElseGet(() -> Stock.ofKospi(item, sector.large(), sector.medium(), sector.small()));
        }

        private SectorInfo resolveSector(String large, String medium, String small, String ticker) {
            return new SectorInfo(large, medium, small);
        }
    }

    // ── KOSDAQ ────────────────────────────────────────────────────────────────

    public static class Kosdaq implements ItemProcessor<KosdaqItem, Stock> {

        private final StockRepository stockRepository;

        public Kosdaq(StockRepository stockRepository) {
            this.stockRepository = stockRepository;
        }

        @Override
        public Stock process(KosdaqItem item) {
            if (item.shortCode() == null || item.shortCode().isBlank()) {
                log.warn("[KOSDAQ] 단축코드 없음, skip: isin={}", item.isinCode());
                return null;
            }

            SectorInfo sector = resolveSector(item.sectorLarge(), item.sectorMedium(), item.sectorSmall(), item.koreanName());

            return stockRepository.findByTicker(item.shortCode())
                    .map(existing -> {
                        existing.updateFromKosdaq(item, sector.large(), sector.medium(), sector.small());
                        return existing;
                    })
                    .orElseGet(() -> Stock.ofKosdaq(item, sector.large(), sector.medium(), sector.small()));
        }

        private SectorInfo resolveSector(String large, String medium, String small, String ticker) {
            return new SectorInfo(large, medium, small);
        }
    }

    // ── 내부 DTO ──────────────────────────────────────────────────────────────
    private record SectorInfo(String large, String medium, String small) {
    }
}