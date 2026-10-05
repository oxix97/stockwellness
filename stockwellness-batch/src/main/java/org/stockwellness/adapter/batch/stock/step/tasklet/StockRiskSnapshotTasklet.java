package org.stockwellness.adapter.batch.stock.step.tasklet;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.stockwellness.adapter.batch.stock.snapshot.StockMasterSnapshot;
import org.stockwellness.adapter.batch.stock.snapshot.StockMasterSnapshotStore;
import org.stockwellness.application.port.in.batch.StockMasterSyncUseCase;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockRiskStatusPort;
import org.stockwellness.application.service.batch.StockRiskStatusMapper;
import org.stockwellness.domain.stock.KosdaqItem;
import org.stockwellness.domain.stock.KospiItem;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockRiskStatus;

@Slf4j
@RequiredArgsConstructor
public class StockRiskSnapshotTasklet implements Tasklet {
    private final MarketType marketType;
    private final StockMasterSnapshot kospiSnapshot;
    private final StockMasterSnapshot kosdaqSnapshot;
    private final StockMasterSnapshotStore snapshotStore;
    private final StockMasterSyncUseCase stockMasterSyncUseCase;
    private final StockPort stockPort;
    private final StockRiskStatusMapper mapper;
    private final StockRiskStatusPort riskStatusPort;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        snapshotStore.verifyBoth(kospiSnapshot, kosdaqSnapshot);
        List<StockRiskStatus> complete = new ArrayList<>();
        int unverified = 0;
        if (marketType == MarketType.KOSPI) {
            for (KospiItem item : stockMasterSyncUseCase.parseKospiMasterArchive(snapshotStore.readVerified(kospiSnapshot))) {
                Stock stock = stockPort.loadStockByTickerAndMarketCode(item.shortCode(), "KOSPI").orElse(null);
                var mapped = mapper.fromKospi(stock, item);
                if (mapped.isPresent()) complete.add(mapped.get()); else unverified++;
            }
        } else {
            for (KosdaqItem item : stockMasterSyncUseCase.parseKosdaqMasterArchive(snapshotStore.readVerified(kosdaqSnapshot))) {
                Stock stock = stockPort.loadStockByTickerAndMarketCode(item.shortCode(), "KOSDAQ").orElse(null);
                var mapped = mapper.fromKosdaq(stock, item);
                if (mapped.isPresent()) complete.add(mapped.get()); else unverified++;
            }
        }
        riskStatusPort.upsert(complete);
        log.info("KIS risk snapshot market={} persisted={}, withheld={} (missing/unsupported fields)",
                marketType, complete.size(), unverified);
        contribution.incrementWriteCount(complete.size());
        return RepeatStatus.FINISHED;
    }
}
