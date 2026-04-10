package org.stockwellness.batch.support.operations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.application.port.in.batch.BatchControlUseCase;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.batch.job.stockmaster.application.MarketIndexSyncService;
import org.stockwellness.batch.support.exception.BatchException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BatchOperationsServiceTest {

    @Mock
    private JobExplorer jobExplorer;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private JobLauncher asyncJobLauncher;

    @Mock
    private JobOperator jobOperator;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private Job stockMasterSyncJob;

    @Mock
    private Job stockPriceBatchJob;

    @Mock
    private Job sectorEodJob;

    @Mock
    private Job stockPricePrevCloseSyncJob;

    @Mock
    private Job portfolioStatsJob;

    @Mock
    private Job benchmarkPriceSyncJob;

    @Mock
    private Job stockInvestorTradeDetailJob;

    @Mock
    private StockPort stockPort;

    @Mock
    private MarketIndexSyncService marketIndexSyncService;

    @Mock
    private StockPriceRepository stockPriceRepository;

    @InjectMocks
    private BatchOperationsService batchOperationsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(batchOperationsService, "asyncJobLauncher", asyncJobLauncher);
        ReflectionTestUtils.setField(batchOperationsService, "jobLauncher", jobLauncher);
        ReflectionTestUtils.setField(batchOperationsService, "stockPriceBatchJob", stockPriceBatchJob);
        ReflectionTestUtils.setField(batchOperationsService, "benchmarkPriceSyncJob", benchmarkPriceSyncJob);
        ReflectionTestUtils.setField(batchOperationsService, "stockPricePrevCloseSyncJob", stockPricePrevCloseSyncJob);
    }

    @Test
    @DisplayName("종목 마스터 동기화는 null startDate와 endDate를 JobParameters에 추가하지 않는다")
    void buildParameters_stockMasterSync_omitsNullDates() {
        JobParameters parameters = ReflectionTestUtils.invokeMethod(batchOperationsService, "buildParameters",
                new BatchControlUseCase.BatchLaunchCommand(
                BatchControlUseCase.BatchJobType.STOCK_MASTER_SYNC,
                null,
                null,
                null,
                false
        ));

        assertThat(parameters.getString("startDate")).isNull();
        assertThat(parameters.getString("endDate")).isNull();
        assertThat(parameters.getLong("time")).isNotNull();
    }

    @Test
    @DisplayName("투자주체 보정은 baseDate가 null이면 해당 파라미터를 JobParameters에 추가하지 않는다")
    void buildParameters_stockForeignInstitution_omitsNullBaseDate() {
        JobParameters parameters = ReflectionTestUtils.invokeMethod(batchOperationsService, "buildParameters",
                new BatchControlUseCase.BatchLaunchCommand(
                BatchControlUseCase.BatchJobType.STOCK_FOREIGN_INSTITUTION,
                null,
                null,
                null,
                false
        ));

        assertThat(parameters.getString("baseDate")).isNull();
        assertThat(parameters.getLong("time")).isNotNull();
    }

    @Test
    @DisplayName("벤치마크 동기화는 명시 날짜가 있을 때만 startDate와 endDate를 JobParameters에 추가한다")
    void buildParameters_benchmarkPriceSync_addsNormalizedDatesWhenPresent() {
        JobParameters parameters = ReflectionTestUtils.invokeMethod(batchOperationsService, "buildParameters",
                new BatchControlUseCase.BatchLaunchCommand(
                BatchControlUseCase.BatchJobType.BENCHMARK_PRICE_SYNC,
                null,
                "2026-04-01",
                "20260408",
                false
        ));

        assertThat(parameters.getString("startDate")).isEqualTo("20260401");
        assertThat(parameters.getString("endDate")).isEqualTo("20260408");
        assertThat(parameters.getLong("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("시세 동기화 배치가 이미 실행 중이면 중복 실행을 차단한다")
    void launchAsync_stockPriceSync_blocksWhenJobAlreadyRunning() throws Exception {
        given(jobExplorer.findRunningJobExecutions("stockPriceBatchJob"))
                .willReturn(Set.of(org.mockito.Mockito.mock(JobExecution.class)));

        BatchControlUseCase.BatchLaunchCommand command = new BatchControlUseCase.BatchLaunchCommand(
                BatchControlUseCase.BatchJobType.STOCK_PRICE_SYNC,
                null,
                null,
                null,
                false
        );

        assertThatThrownBy(() -> batchOperationsService.launchAsync(command))
                .isInstanceOfSatisfying(BatchException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(org.stockwellness.global.error.ErrorCode.BATCH_JOB_ALREADY_RUNNING));

        verify(asyncJobLauncher, never()).run(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("KIS 호출 배치는 이미 실행 중이면 중복 실행을 차단한다")
    void launchAsync_benchmarkSync_blocksWhenJobAlreadyRunning() throws Exception {
        given(jobExplorer.findRunningJobExecutions("benchmarkPriceSyncJob"))
                .willReturn(Set.of(org.mockito.Mockito.mock(JobExecution.class)));

        BatchControlUseCase.BatchLaunchCommand command = new BatchControlUseCase.BatchLaunchCommand(
                BatchControlUseCase.BatchJobType.BENCHMARK_PRICE_SYNC,
                null,
                "20260401",
                "20260408",
                false
        );

        assertThatThrownBy(() -> batchOperationsService.launchAsync(command))
                .isInstanceOfSatisfying(BatchException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(org.stockwellness.global.error.ErrorCode.BATCH_JOB_ALREADY_RUNNING));

        verify(asyncJobLauncher, never()).run(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("DB 보정 배치는 다른 실행이 있어도 중복 실행 차단 대상이 아니다")
    void launchAsync_prevCloseSync_doesNotBlockWhenJobAlreadyRunning() throws Exception {
        JobExecution jobExecution = org.mockito.Mockito.mock(JobExecution.class);
        given(jobExecution.getId()).willReturn(99L);

        org.springframework.batch.core.JobInstance jobInstance = org.mockito.Mockito.mock(org.springframework.batch.core.JobInstance.class);
        given(jobExecution.getJobInstance()).willReturn(jobInstance);
        given(jobInstance.getJobName()).willReturn("stockPricePrevCloseSyncJob");
        given(jobExecution.getStatus()).willReturn(org.springframework.batch.core.BatchStatus.COMPLETED);
        given(asyncJobLauncher.run(org.mockito.ArgumentMatchers.eq(stockPricePrevCloseSyncJob), org.mockito.ArgumentMatchers.any()))
                .willReturn(jobExecution);

        BatchControlUseCase.BatchLaunchCommand command = new BatchControlUseCase.BatchLaunchCommand(
                BatchControlUseCase.BatchJobType.STOCK_PRICE_PREV_CLOSE_SYNC,
                null,
                "20260401",
                "20260408",
                false
        );

        BatchControlUseCase.BatchExecutionResult result = batchOperationsService.launchAsync(command);

        assertThat(result.jobName()).isEqualTo("stockPricePrevCloseSyncJob");
        verifyNoInteractions(jobExplorer);
        verify(asyncJobLauncher).run(org.mockito.ArgumentMatchers.eq(stockPricePrevCloseSyncJob), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("abandon은 실행 중인 배치를 FAILED 상태로 변경한다")
    void abandon_marksRunningJobAsFailed() {
        Long executionId = 1L;
        JobExecution jobExecution = mock(JobExecution.class);
        StepExecution stepExecution = mock(StepExecution.class);

        given(jobExplorer.getJobExecution(executionId)).willReturn(jobExecution);
        given(jobExecution.getStatus()).willReturn(BatchStatus.STARTED);
        given(jobExecution.getStepExecutions()).willReturn(List.of(stepExecution));
        given(stepExecution.getStatus()).willReturn(BatchStatus.STARTED);

        String result = batchOperationsService.abandon(executionId);

        assertThat(result).contains("강제 종료 처리했습니다");
        verify(jobExecution).setStatus(BatchStatus.FAILED);
        verify(stepExecution).setStatus(BatchStatus.FAILED);
        verify(jobRepository).update(jobExecution);
        verify(jobRepository).update(stepExecution);
    }

    @Test
    @DisplayName("cleanupStuckJobs는 모든 실행 중인 배치를 FAILED 상태로 변경한다")
    void cleanupStuckJobs_marksAllRunningJobsAsFailed() {
        String jobName = "testJob";
        JobExecution jobExecution = mock(JobExecution.class);
        given(jobExplorer.getJobNames()).willReturn(List.of(jobName));
        given(jobExplorer.findRunningJobExecutions(jobName)).willReturn(Set.of(jobExecution));
        given(jobExecution.getStepExecutions()).willReturn(List.of());

        batchOperationsService.cleanupStuckJobs();

        verify(jobExecution).setStatus(BatchStatus.FAILED);
        verify(jobRepository).update(jobExecution);
    }
}
