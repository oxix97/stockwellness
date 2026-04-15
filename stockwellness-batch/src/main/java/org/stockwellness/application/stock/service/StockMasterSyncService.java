package org.stockwellness.application.stock.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.stockwellness.adapter.out.external.kis.client.KisMasterClient;
import org.stockwellness.adapter.out.persistence.stock.repository.MarketIndexRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.application.port.in.batch.StockMasterSyncUseCase;
import org.stockwellness.application.stock.parser.KosdaqMstParser;
import org.stockwellness.application.stock.parser.KospiMstParser;
import org.stockwellness.domain.stock.KosdaqItem;
import org.stockwellness.domain.stock.KospiItem;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockSector;
import org.stockwellness.domain.stock.insight.MarketIndex;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockMasterSyncService implements StockMasterSyncUseCase {

    private final KisMasterClient kisMasterClient;
    private final StockRepository stockRepository;
    private final MarketIndexRepository marketIndexRepository;

    @Override
    public List<KospiItem> loadKospiItems() {
        log.info("[KOSPI] 마스터 파일 다운로드 시작");
        List<KospiItem> items = KospiMstParser.parseLines(kisMasterClient.downloadKospiMaster());
        log.info("[KOSPI] 파싱 완료: {}건", items.size());
        return items;
    }

    @Override
    public List<KosdaqItem> loadKosdaqItems() {
        log.info("[KOSDAQ] 마스터 파일 다운로드 시작");
        List<KosdaqItem> items = KosdaqMstParser.parseLines(kisMasterClient.downloadKosdaqMaster());
        log.info("[KOSDAQ] 파싱 완료: {}건", items.size());
        return items;
    }

    @Override
    public StockMasterSyncResult upsertKospi(KospiMasterSyncCommand command) {
        KospiItem item = command.item();
        if (item.shortCode() == null || item.shortCode().isBlank()) {
            log.warn("[KOSPI] 단축코드 없음, skip: isin={}", item.isinCode());
            return new StockMasterSyncResult(null, false);
        }

        Map<String, MarketIndex> indexMap = marketIndexRepository.findActiveIndexMap();
        StockSector sector = StockSector.of(
                item.sectorLarge(),
                item.sectorMedium(),
                item.sectorSmall(),
                indexMap
        );

        return stockRepository.findByTicker(item.shortCode())
                .map(existing -> {
                    existing.updateFromKospi(item, sector);
                    return new StockMasterSyncResult(existing, false);
                })
                .orElseGet(() -> new StockMasterSyncResult(Stock.ofKospi(item, sector), true));
    }

    @Override
    public StockMasterSyncResult upsertKosdaq(KosdaqMasterSyncCommand command) {
        KosdaqItem item = command.item();
        if (item.shortCode() == null || item.shortCode().isBlank()) {
            log.warn("[KOSDAQ] 단축코드 없음, skip: isin={}", item.isinCode());
            return new StockMasterSyncResult(null, false);
        }

        Map<String, MarketIndex> indexMap = marketIndexRepository.findActiveIndexMap();
        StockSector sector = StockSector.of(
                item.sectorLarge(),
                item.sectorMedium(),
                item.sectorSmall(),
                indexMap
        );

        return stockRepository.findByTicker(item.shortCode())
                .map(existing -> {
                    existing.updateFromKosdaq(item, sector);
                    return new StockMasterSyncResult(existing, false);
                })
                .orElseGet(() -> new StockMasterSyncResult(Stock.ofKosdaq(item, sector), true));
    }

    @Override
    public StockDelistResult markDelisted(StockDelistCommand command) {
        if (command.activeTickers() == null || command.activeTickers().isEmpty()) {
            log.warn("[{}] 활성 ticker Set이 비어있어 상장폐지 처리를 건너뜁니다.", command.marketType());
            return new StockDelistResult(command.marketType(), 0);
        }
        int count = stockRepository.delistMissingStocks(command.marketType(), command.activeTickers());
        return new StockDelistResult(command.marketType(), count);
    }
}
