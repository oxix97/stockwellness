package org.stockwellness.application.port.in.batch;

import java.util.List;
import java.util.Set;

import org.stockwellness.domain.stock.KosdaqItem;
import org.stockwellness.domain.stock.KospiItem;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;

public interface StockMasterSyncUseCase {

    byte[] downloadKospiMasterArchive();

    byte[] downloadKosdaqMasterArchive();

    List<KospiItem> parseKospiMasterArchive(byte[] archiveBytes);

    List<KosdaqItem> parseKosdaqMasterArchive(byte[] archiveBytes);

    List<KospiItem> loadKospiItems();

    List<KosdaqItem> loadKosdaqItems();

    StockMasterSyncResult upsertKospi(KospiMasterSyncCommand command);

    StockMasterSyncResult upsertKosdaq(KosdaqMasterSyncCommand command);

    StockDelistResult markDelisted(StockDelistCommand command);

    record KospiMasterSyncCommand(KospiItem item) {
    }

    record KosdaqMasterSyncCommand(KosdaqItem item) {
    }

    record StockDelistCommand(
            MarketType marketType,
            Set<String> activeTickers
    ) {
    }

    record StockMasterSyncResult(
            Stock stock,
            boolean created
    ) {
    }

    record StockDelistResult(
            MarketType marketType,
            int delistedCount
    ) {
    }
}
