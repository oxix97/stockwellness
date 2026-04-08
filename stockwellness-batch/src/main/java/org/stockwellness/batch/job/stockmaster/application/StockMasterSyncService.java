package org.stockwellness.batch.job.stockmaster.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.stockwellness.adapter.out.external.kis.client.KisMasterClient;
import org.stockwellness.adapter.out.persistence.stock.repository.MarketIndexRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.application.port.in.batch.StockMasterSyncUseCase;
import org.stockwellness.domain.stock.KosdaqItem;
import org.stockwellness.domain.stock.KospiItem;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockSector;
import org.stockwellness.domain.stock.insight.MarketIndex;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockMasterSyncService implements StockMasterSyncUseCase {

    private static final Charset CP949 = Charset.forName("EUC-KR");

    private final KisMasterClient kisMasterClient;
    private final StockRepository stockRepository;
    private final MarketIndexRepository marketIndexRepository;

    @Override
    public List<KospiItem> loadKospiItems() {
        log.info("[KOSPI] 마스터 파일 다운로드 시작");
        List<KospiItem> items = kisMasterClient.downloadKospiMaster().stream()
                .map(StockMasterSyncService::parseKospiLine)
                .toList();
        log.info("[KOSPI] 파싱 완료: {}건", items.size());
        return items;
    }

    @Override
    public List<KosdaqItem> loadKosdaqItems() {
        log.info("[KOSDAQ] 마스터 파일 다운로드 시작");
        List<KosdaqItem> items = kisMasterClient.downloadKosdaqMaster().stream()
                .map(StockMasterSyncService::parseKosdaqLine)
                .toList();
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

    private static KospiItem parseKospiLine(String line) {
        int[] widths = {
                2, 1, 4, 4, 4, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 9, 5, 5, 1, 1, 1, 2, 1, 1, 1, 2, 2, 2, 3, 1, 3, 12, 12, 8, 15, 21, 2, 7, 1, 1, 1, 1, 1,
                9, 9, 9, 5, 9, 8, 9, 3, 1, 1, 1
        };
        byte[] lineBytes = line.getBytes(CP949);
        int part2Length = 227;
        byte[] part1Bytes = Arrays.copyOf(lineBytes, lineBytes.length - part2Length);
        byte[] part2Bytes = Arrays.copyOfRange(lineBytes, lineBytes.length - part2Length, lineBytes.length);
        String[] f = splitFixedWidth(part2Bytes, widths);
        return new KospiItem(
                decode(part1Bytes, 0, 9).strip(),
                decode(part1Bytes, 9, 21).strip(),
                decode(part1Bytes, 21, part1Bytes.length).strip(),
                f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9], f[10], f[11], f[12], f[13], f[14],
                f[15], f[16], f[17], f[18], f[19], f[20], f[21], f[22], f[23], f[24], f[25], f[26], f[27], f[28],
                f[29], f[30], f[31], f[32], f[33], f[34], f[35], f[36], f[37], f[38], f[39], f[40], f[41], f[42],
                f[43], f[44], f[45], f[46], f[47], f[48], f[49], f[50], f[51], f[52], f[53], f[54], f[55], f[56],
                f[57], f[58], f[59], f[60], f[61], f[62], f[63], f[64], f[65], f[66], f[67], f[68], f[69]
        );
    }

    private static KosdaqItem parseKosdaqLine(String line) {
        int[] widths = {
                2, 1, 4, 4, 4, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                9, 5, 5, 1, 1, 1, 2, 1, 1, 1, 2, 2, 2, 3, 1, 3, 12, 12, 8, 15, 21, 2, 7, 1, 1, 1, 1,
                9, 9, 9, 5, 9, 8, 9, 3, 1, 1, 1
        };
        byte[] lineBytes = line.getBytes(CP949);
        int part2Length = 221;
        byte[] part1Bytes = Arrays.copyOf(lineBytes, lineBytes.length - part2Length);
        byte[] part2Bytes = Arrays.copyOfRange(lineBytes, lineBytes.length - part2Length, lineBytes.length);
        String[] f = splitFixedWidth(part2Bytes, widths);
        return new KosdaqItem(
                decode(part1Bytes, 0, 9).strip(),
                decode(part1Bytes, 9, 21).strip(),
                decode(part1Bytes, 21, part1Bytes.length).strip(),
                f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9], f[10], f[11], f[12], f[13], f[14],
                f[15], f[16], f[17], f[18], f[19], f[20], f[21], f[22], f[23], f[24], f[25], f[26], f[27], f[28],
                f[29], f[30], f[31], f[32], f[33], f[34], f[35], f[36], f[37], f[38], f[39], f[40], f[41], f[42],
                f[43], f[44], f[45], f[46], f[47], f[48], f[49], f[50], f[51], f[52], f[53], f[54], f[55], f[56],
                f[57], f[58], f[59], f[60], f[61], f[62], f[63]
        );
    }

    private static String[] splitFixedWidth(byte[] data, int[] widths) {
        String[] fields = new String[widths.length];
        int offset = 0;
        for (int i = 0; i < widths.length; i++) {
            int end = Math.min(offset + widths[i], data.length);
            fields[i] = new String(data, offset, end - offset, CP949).strip();
            offset += widths[i];
        }
        return fields;
    }

    private static String decode(byte[] bytes, int from, int to) {
        int end = Math.min(to, bytes.length);
        return new String(bytes, from, end - from, CP949);
    }
}
