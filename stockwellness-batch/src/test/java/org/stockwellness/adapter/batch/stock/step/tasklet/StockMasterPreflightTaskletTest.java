package org.stockwellness.adapter.batch.stock.step.tasklet;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.explore.JobExplorer;
import org.stockwellness.adapter.batch.stock.config.StockMasterSnapshotProperties;
import org.stockwellness.adapter.batch.stock.snapshot.StockMasterSnapshot;
import org.stockwellness.adapter.batch.stock.snapshot.StockMasterSnapshotStore;
import org.stockwellness.application.port.in.batch.StockMasterSyncUseCase;
import org.stockwellness.application.parser.KosdaqMstParser;
import org.stockwellness.application.parser.KospiMstParser;
import org.stockwellness.domain.stock.MarketType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockMasterPreflightTaskletTest {

    private static final Charset CP949 = Charset.forName("CP949");

    @TempDir
    Path stagingDirectory;

    private StockMasterSyncUseCase syncUseCase;
    private JobExplorer jobExplorer;
    private StockMasterSnapshotStore snapshotStore;
    private StockMasterPreflightTasklet tasklet;
    private JobExecution jobExecution;

    @BeforeEach
    void setUp() {
        syncUseCase = mock(StockMasterSyncUseCase.class);
        jobExplorer = mock(JobExplorer.class);
        snapshotStore = new StockMasterSnapshotStore(
                new StockMasterSnapshotProperties(stagingDirectory, Duration.ofDays(7)));
        tasklet = new StockMasterPreflightTasklet(syncUseCase, snapshotStore, jobExplorer,
                new StockMasterSnapshotProperties(stagingDirectory, Duration.ofDays(7)));
        jobExecution = new JobExecution(new JobInstance(1L, "stockMasterSyncJob"), 42L, null);
        when(jobExplorer.findRunningJobExecutions("stockMasterSyncJob")).thenReturn(Set.of());
    }

    @Test
    void validArchivesPublishBothVerifiedDescriptorsOnlyAfterParsingBoth() throws Exception {
        byte[] kospi = archive("kospi_code.mst", row(288, "005930", "KR7005930003", "삼성전자"));
        byte[] kosdaq = archive("kosdaq_code.mst", row(281, "035900", "KR7035900000", "JYP Ent."));
        when(syncUseCase.downloadKospiMasterArchive()).thenReturn(kospi);
        when(syncUseCase.downloadKosdaqMasterArchive()).thenReturn(kosdaq);
        when(syncUseCase.parseKospiMasterArchive(kospi)).thenReturn(KospiMstParser.parseArchive(kospi));
        when(syncUseCase.parseKosdaqMasterArchive(kosdaq)).thenReturn(KosdaqMstParser.parseArchive(kosdaq));

        tasklet.execute(contribution(), chunkContext());

        var context = jobExecution.getExecutionContext();
        StockMasterSnapshot kospiSnapshot = (StockMasterSnapshot) context.get("stockMaster.kospi.snapshot");
        StockMasterSnapshot kosdaqSnapshot = (StockMasterSnapshot) context.get("stockMaster.kosdaq.snapshot");
        assertThat(kospiSnapshot.recordCount()).isEqualTo(1);
        assertThat(kosdaqSnapshot.recordCount()).isEqualTo(1);
        assertThat(kospiSnapshot.completenessStatus()).isEqualTo("STRUCTURALLY_VALID");
        assertThat(kosdaqSnapshot.completenessStatus()).isEqualTo("STRUCTURALLY_VALID");
        snapshotStore.verifyBoth(kospiSnapshot, kosdaqSnapshot);
        verify(syncUseCase).parseKospiMasterArchive(kospi);
        verify(syncUseCase).parseKosdaqMasterArchive(kosdaq);
    }

    @Test
    void invalidKospiPublishesNoDescriptorAndStagesNoArchive() {
        when(syncUseCase.downloadKospiMasterArchive()).thenReturn(new byte[]{1, 2, 3});
        when(syncUseCase.downloadKosdaqMasterArchive()).thenReturn(new byte[]{4, 5, 6});
        when(syncUseCase.parseKospiMasterArchive(new byte[]{1, 2, 3}))
                .thenThrow(new IllegalArgumentException("invalid KOSPI"));

        assertThatThrownBy(() -> tasklet.execute(contribution(), chunkContext()))
                .isInstanceOf(IllegalStateException.class);

        assertNoSnapshotsOrArtifacts();
        verify(syncUseCase).downloadKosdaqMasterArchive();
        verify(syncUseCase, never()).parseKosdaqMasterArchive(new byte[]{4, 5, 6});
    }

    @Test
    void invalidKosdaqDoesNotLeaveKospiEligibleForProcessing() throws Exception {
        byte[] kospi = archive("kospi_code.mst", row(288, "005930", "KR7005930003", "삼성전자"));
        when(syncUseCase.downloadKospiMasterArchive()).thenReturn(kospi);
        when(syncUseCase.downloadKosdaqMasterArchive()).thenReturn(new byte[]{4, 5, 6});
        when(syncUseCase.parseKospiMasterArchive(kospi)).thenReturn(KospiMstParser.parseArchive(kospi));
        when(syncUseCase.parseKosdaqMasterArchive(new byte[]{4, 5, 6}))
                .thenThrow(new IllegalArgumentException("invalid KOSDAQ"));

        assertThatThrownBy(() -> tasklet.execute(contribution(), chunkContext()))
                .isInstanceOf(IllegalStateException.class);

        assertNoSnapshotsOrArtifacts();
        verify(syncUseCase).parseKospiMasterArchive(kospi);
        verify(syncUseCase).parseKosdaqMasterArchive(new byte[]{4, 5, 6});
    }

    @Test
    void restartReusesAndVerifiesPreviouslyPublishedSnapshots() throws Exception {
        byte[] kospi = archive("kospi_code.mst", row(288, "005930", "KR7005930003", "삼성전자"));
        byte[] kosdaq = archive("kosdaq_code.mst", row(281, "035900", "KR7035900000", "JYP Ent."));
        StockMasterSnapshot kospiSnapshot = snapshotStore.stage(42L, MarketType.KOSPI, kospi, 1, "STRUCTURALLY_VALID");
        StockMasterSnapshot kosdaqSnapshot = snapshotStore.stage(42L, MarketType.KOSDAQ, kosdaq, 1, "STRUCTURALLY_VALID");
        jobExecution.getExecutionContext().put("stockMaster.kospi.snapshot", kospiSnapshot);
        jobExecution.getExecutionContext().put("stockMaster.kosdaq.snapshot", kosdaqSnapshot);

        tasklet.execute(contribution(), chunkContext());

        verify(syncUseCase, never()).downloadKospiMasterArchive();
        verify(syncUseCase, never()).downloadKosdaqMasterArchive();
        snapshotStore.verifyBoth(kospiSnapshot, kosdaqSnapshot);
    }

    private StepContribution contribution() {
        StepExecution stepExecution = new StepExecution("stockMasterPreflightStep", jobExecution);
        return new StepContribution(stepExecution);
    }

    private ChunkContext chunkContext() {
        StepExecution stepExecution = new StepExecution("stockMasterPreflightStep", jobExecution);
        return new ChunkContext(new StepContext(stepExecution));
    }

    private void assertNoSnapshotsOrArtifacts() {
        assertThat(jobExecution.getExecutionContext().containsKey("stockMaster.kospi.snapshot")).isFalse();
        assertThat(jobExecution.getExecutionContext().containsKey("stockMaster.kosdaq.snapshot")).isFalse();
        assertThat(stagingDirectory.resolve("42")).doesNotExist();
    }

    private static byte[] row(int totalWidth, String ticker, String isin, String name) {
        int tickerWidth = 9;
        int isinWidth = 12;
        int nameWidth = totalWidth == 288 ? 40 : 39;
        ByteArrayOutputStream row = new ByteArrayOutputStream(totalWidth);
        writePadded(row, ticker, tickerWidth);
        writePadded(row, isin, isinWidth);
        writePadded(row, name, nameWidth);
        row.writeBytes(" ".repeat(totalWidth - tickerWidth - isinWidth - nameWidth).getBytes(CP949));
        return row.toByteArray();
    }

    private static void writePadded(ByteArrayOutputStream target, String value, int width) {
        byte[] bytes = value.getBytes(CP949);
        target.writeBytes(bytes);
        target.writeBytes(" ".repeat(width - bytes.length).getBytes(CP949));
    }

    private static byte[] archive(String name, byte[] row) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(row);
            zip.write('\n');
            zip.closeEntry();
        }
        return output.toByteArray();
    }
}
