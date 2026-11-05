package org.stockwellness.batch.job.stockmaster.config;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.stockwellness.adapter.out.external.kis.client.KisMasterClient;
import org.stockwellness.adapter.out.persistence.stock.repository.MarketIndexRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.adapter.batch.stock.snapshot.StockMasterSnapshot;
import org.stockwellness.batch.job.stockmaster.support.StockMasterArchiveFixture;
import org.stockwellness.application.service.batch.StockMasterSyncService;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.support.BatchIntegrationTestSupport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class StockMasterSyncJobTest extends BatchIntegrationTestSupport {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private MarketIndexRepository marketIndexRepository;

    @Autowired
    private KisMasterClient kisMasterClient;

    @MockitoSpyBean
    private StockMasterSyncService stockMasterSyncService;

    @Autowired
    @Qualifier("stockMasterSyncJob")
    private Job stockMasterSyncJob;

    @BeforeEach
    void setUp() {
        stockRepository.deleteAllInBatch();
        marketIndexRepository.deleteAllInBatch();
    }

    @Test
    void structurallyValidSnapshotsUpsertBothMarketsWithoutDelistingExistingRows() throws Exception {
        Stock missingKospi = Stock.of("999998", "KR7999998003", "legacy KOSPI",
                MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE);
        Stock missingKosdaq = Stock.of("999999", "KR7999999001", "legacy KOSDAQ",
                MarketType.KOSDAQ, Currency.KRW, null, StockStatus.ACTIVE);
        stockRepository.saveAllAndFlush(List.of(missingKospi, missingKosdaq));

        given(kisMasterClient.downloadKospiMasterArchive()).willReturn(StockMasterArchiveFixture.kospi(List.of(
                new StockMasterArchiveFixture.Row("005930", "KR7005930003", "삼성전자"))));
        given(kisMasterClient.downloadKosdaqMasterArchive()).willReturn(StockMasterArchiveFixture.kosdaq(List.of(
                new StockMasterArchiveFixture.Row("035900", "KR7035900000", "JYP Ent."))));

        JobExecution execution = runWithUniqueParameters();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(stockRepository.findByTickerAndMarketCode("005930", "KOSPI")).isPresent();
        assertThat(stockRepository.findByTickerAndMarketCode("035900", "KOSDAQ")).isPresent();
        assertThat(stockRepository.findByTickerAndMarketCode("999998", "KOSPI"))
                .get().extracting(Stock::getStatus).isEqualTo(StockStatus.ACTIVE);
        assertThat(stockRepository.findByTickerAndMarketCode("999999", "KOSDAQ"))
                .get().extracting(Stock::getStatus).isEqualTo(StockStatus.ACTIVE);
    }

    @Test
    void validKOSPIAndInvalidKOSDAQFailBeforeAnyStockWrite() throws Exception {
        seedLegacyStocks();
        givenValidKospi();
        given(kisMasterClient.downloadKosdaqMasterArchive()).willReturn(new byte[]{1, 2, 3});

        JobExecution execution = runWithUniqueParameters();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertNoMasterWrites();
    }

    @Test
    void validKOSDAQAndInvalidKOSPIFailBeforeAnyStockWrite() throws Exception {
        seedLegacyStocks();
        given(kisMasterClient.downloadKospiMasterArchive()).willReturn(new byte[]{1, 2, 3});
        givenValidKosdaq();

        JobExecution execution = runWithUniqueParameters();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertNoMasterWrites();
    }

    @Test
    void restartReusesTheStagedArchivesWithoutDownloadingAgain() throws Exception {
        givenValidKospi();
        givenValidKosdaq();
        AtomicBoolean failOnce = new AtomicBoolean(true);
        doAnswer(invocation -> {
            if (failOnce.getAndSet(false)) {
                throw new IllegalStateException("one-shot test failure after preflight");
            }
            return invocation.callRealMethod();
        }).when(stockMasterSyncService).upsertKospi(any());
        var parameters = uniqueParameters();

        JobExecution first = jobLauncher.run(stockMasterSyncJob, parameters);
        assertThat(first.getStatus()).isEqualTo(BatchStatus.FAILED);
        StockMasterSnapshot firstKospi = snapshot(first, "stockMaster.kospi.snapshot");
        StockMasterSnapshot firstKosdaq = snapshot(first, "stockMaster.kosdaq.snapshot");

        JobExecution restarted = jobLauncher.run(stockMasterSyncJob, parameters);

        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(snapshot(restarted, "stockMaster.kospi.snapshot")).isEqualTo(firstKospi);
        assertThat(snapshot(restarted, "stockMaster.kosdaq.snapshot")).isEqualTo(firstKosdaq);
        verify(kisMasterClient, times(1)).downloadKospiMasterArchive();
        verify(kisMasterClient, times(1)).downloadKosdaqMasterArchive();
    }

    @Test
    void restartFailsBeforeUpsertWhenEitherStagedArchiveHashChanges() throws Exception {
        givenValidKospi();
        givenValidKosdaq();
        AtomicBoolean failOnce = new AtomicBoolean(true);
        doAnswer(invocation -> {
            if (failOnce.getAndSet(false)) {
                throw new IllegalStateException("one-shot test failure after preflight");
            }
            return invocation.callRealMethod();
        }).when(stockMasterSyncService).upsertKospi(any());
        var parameters = uniqueParameters();

        JobExecution first = jobLauncher.run(stockMasterSyncJob, parameters);
        assertThat(first.getStatus()).isEqualTo(BatchStatus.FAILED);
        StockMasterSnapshot kosdaqSnapshot = snapshot(first, "stockMaster.kosdaq.snapshot");
        byte[] tamperedArchive = Files.readAllBytes(Path.of(kosdaqSnapshot.artifactPath()));
        tamperedArchive[0] ^= 0x01;
        Files.write(Path.of(kosdaqSnapshot.artifactPath()), tamperedArchive);

        JobExecution restarted = jobLauncher.run(stockMasterSyncJob, parameters);

        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.FAILED);
        verify(stockMasterSyncService, times(1)).upsertKospi(any());
        verify(kisMasterClient, times(1)).downloadKospiMasterArchive();
        verify(kisMasterClient, times(1)).downloadKosdaqMasterArchive();
        assertThat(stockRepository.findByTickerAndMarketCode("005930", "KOSPI")).isEmpty();
        assertThat(stockRepository.findByTickerAndMarketCode("035900", "KOSDAQ")).isEmpty();
    }

    private JobExecution runWithUniqueParameters() throws Exception {
        return jobLauncher.run(stockMasterSyncJob, uniqueParameters());
    }

    private static org.springframework.batch.core.JobParameters uniqueParameters() {
        return new JobParametersBuilder().addString("testRunId", UUID.randomUUID().toString()).toJobParameters();
    }

    private void seedLegacyStocks() {
        stockRepository.saveAllAndFlush(List.of(
                Stock.of("999998", "KR7999998003", "legacy KOSPI",
                        MarketType.KOSPI, Currency.KRW, null, StockStatus.ACTIVE),
                Stock.of("999999", "KR7999999001", "legacy KOSDAQ",
                        MarketType.KOSDAQ, Currency.KRW, null, StockStatus.ACTIVE)));
    }

    private void assertNoMasterWrites() {
        assertThat(stockRepository.count()).isEqualTo(2);
        assertThat(stockRepository.findByTickerAndMarketCode("999998", "KOSPI"))
                .get().extracting(Stock::getStatus).isEqualTo(StockStatus.ACTIVE);
        assertThat(stockRepository.findByTickerAndMarketCode("999999", "KOSDAQ"))
                .get().extracting(Stock::getStatus).isEqualTo(StockStatus.ACTIVE);
        assertThat(stockRepository.findByTickerAndMarketCode("005930", "KOSPI")).isEmpty();
        assertThat(stockRepository.findByTickerAndMarketCode("035900", "KOSDAQ")).isEmpty();
    }

    private void givenValidKospi() {
        given(kisMasterClient.downloadKospiMasterArchive()).willReturn(StockMasterArchiveFixture.kospi(List.of(
                new StockMasterArchiveFixture.Row("005930", "KR7005930003", "삼성전자"))));
    }

    private void givenValidKosdaq() {
        given(kisMasterClient.downloadKosdaqMasterArchive()).willReturn(StockMasterArchiveFixture.kosdaq(List.of(
                new StockMasterArchiveFixture.Row("035900", "KR7035900000", "JYP Ent."))));
    }

    private static StockMasterSnapshot snapshot(JobExecution execution, String key) {
        return (StockMasterSnapshot) execution.getExecutionContext().get(key);
    }
}
