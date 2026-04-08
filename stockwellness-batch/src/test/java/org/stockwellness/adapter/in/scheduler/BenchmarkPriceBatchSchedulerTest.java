package org.stockwellness.adapter.in.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.batch.BatchControlUseCase;
import org.stockwellness.global.util.DateUtil;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BenchmarkPriceBatchSchedulerTest {

    @Mock
    private BatchControlUseCase batchControlUseCase;

    @InjectMocks
    private BenchmarkPriceBatchScheduler benchmarkPriceBatchScheduler;

    @Test
    @DisplayName("자동 지수 시세 배치는 직전 영업일 기준 최근 14일 범위를 수집한다")
    void runBenchmarkPriceSyncJob_usesLookbackWindowEndingAtPreviousBusinessDay() {
        given(batchControlUseCase.launchSync(any()))
                .willReturn(new BatchControlUseCase.BatchExecutionResult(1L, "benchmarkPriceSyncJob", "COMPLETED", null, "ok"));

        benchmarkPriceBatchScheduler.runBenchmarkPriceSyncJob();

        ArgumentCaptor<BatchControlUseCase.BatchLaunchCommand> captor =
                ArgumentCaptor.forClass(BatchControlUseCase.BatchLaunchCommand.class);
        verify(batchControlUseCase).launchSync(captor.capture());

        LocalDate expectedEndDate = DateUtil.previousBusinessDay(LocalDate.now());
        LocalDate expectedStartDate = expectedEndDate.minusDays(14);
        BatchControlUseCase.BatchLaunchCommand command = captor.getValue();

        assertThat(command.jobType()).isEqualTo(BatchControlUseCase.BatchJobType.BENCHMARK_PRICE_SYNC);
        assertThat(command.startDate()).isEqualTo(DateUtil.format(expectedStartDate));
        assertThat(command.endDate()).isEqualTo(DateUtil.format(expectedEndDate));
    }

    @Test
    @DisplayName("배치가 COMPLETED가 아니면 성공 로그 대신 실패로 처리한다")
    void runBenchmarkPriceSyncJob_throwsWhenBatchStatusIsNotCompleted() {
        given(batchControlUseCase.launchSync(any()))
                .willReturn(new BatchControlUseCase.BatchExecutionResult(1L, "benchmarkPriceSyncJob", "FAILED", null, "failed"));

        benchmarkPriceBatchScheduler.runBenchmarkPriceSyncJob();

        verify(batchControlUseCase).launchSync(any());
    }
}
