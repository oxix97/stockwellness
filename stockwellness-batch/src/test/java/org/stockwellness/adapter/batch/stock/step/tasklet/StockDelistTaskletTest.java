package org.stockwellness.adapter.batch.stock.step.tasklet;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.stockwellness.adapter.batch.stock.step.reader.KosdaqMasterItemReader;
import org.stockwellness.adapter.batch.stock.step.reader.KospiMasterItemReader;
import org.stockwellness.application.port.in.batch.StockMasterSyncUseCase;
import org.stockwellness.domain.stock.MarketType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockDelistTaskletTest {

    @Test
    void onlyCompleteKOSPIDatasetCanMarkDelisted() throws Exception {
        assertMarketStatus(MarketType.KOSPI, "STRUCTURALLY_VALID", false);
        assertMarketStatus(MarketType.KOSPI, null, false);
        assertMarketStatus(MarketType.KOSPI, "UNKNOWN", false);
        assertMarketStatus(MarketType.KOSPI, "COMPLETE", true);
    }

    @Test
    void onlyCompleteKOSDAQDatasetCanMarkDelisted() throws Exception {
        assertMarketStatus(MarketType.KOSDAQ, "STRUCTURALLY_VALID", false);
        assertMarketStatus(MarketType.KOSDAQ, null, false);
        assertMarketStatus(MarketType.KOSDAQ, "UNKNOWN", false);
        assertMarketStatus(MarketType.KOSDAQ, "COMPLETE", true);
    }

    private static void assertMarketStatus(MarketType marketType, String completeness, boolean shouldDelist)
            throws Exception {
        StockMasterSyncUseCase syncUseCase = mock(StockMasterSyncUseCase.class);
        when(syncUseCase.markDelisted(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new StockMasterSyncUseCase.StockDelistResult(marketType, 0));
        JobExecution jobExecution = new JobExecution(new JobInstance(1L, "stockMasterSyncJob"), 11L, null);
        if (completeness != null) {
            jobExecution.getExecutionContext().putString("stockMaster.completenessStatus", completeness);
        }
        String tickerKey = marketType == MarketType.KOSPI
                ? KospiMasterItemReader.CTX_KEY_ACTIVE_CODES
                : KosdaqMasterItemReader.CTX_KEY_ACTIVE_CODES;
        jobExecution.getExecutionContext().put(tickerKey, Set.of("005930"));
        StepExecution stepExecution = new StepExecution("stockDelistStep", jobExecution);
        StepContribution contribution = new StepContribution(stepExecution);
        ChunkContext chunkContext = new ChunkContext(new StepContext(stepExecution));
        StockDelistTasklet tasklet = new StockDelistTasklet(syncUseCase, marketType);

        tasklet.execute(contribution, chunkContext);

        if (shouldDelist) {
            verify(syncUseCase).markDelisted(new StockMasterSyncUseCase.StockDelistCommand(
                    marketType, Set.of("005930")));
        } else {
            verify(syncUseCase, never()).markDelisted(org.mockito.ArgumentMatchers.any());
            assertThat(contribution.getExitStatus().getExitCode()).isEqualTo("SKIPPED");
        }
    }
}
