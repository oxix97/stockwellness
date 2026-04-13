package org.stockwellness.adapter.in.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.batch.BatchControlUseCase;
import org.stockwellness.batch.support.exception.BatchException;
import org.stockwellness.global.util.DateUtil;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailyBatchOrchestrationServiceTest {

    @Mock
    private BatchControlUseCase batchControlUseCase;

    @InjectMocks
    private DailyBatchOrchestrationService dailyBatchOrchestrationService;

    @Test
    @DisplayName("일일 전체 동기화는 정의된 6개 배치를 순서대로 실행한다")
    void runDailyFullSync_runsStepsInOrder() {
        given(batchControlUseCase.launchSync(any()))
                .willReturn(completed("stockMasterSyncJob"))
                .willReturn(completed("stockPriceBatchJob"))
                .willReturn(completed("benchmarkPriceSyncJob"))
                .willReturn(completed("stockInvestorTradeDetailJob"))
                .willReturn(completed("sectorEodJob"))
                .willReturn(completed("portfolioStatsJob"));

        LocalDate businessDate = LocalDate.of(2026, 4, 10);
        dailyBatchOrchestrationService.runDailyFullSync(businessDate);

        ArgumentCaptor<BatchControlUseCase.BatchLaunchCommand> captor =
                ArgumentCaptor.forClass(BatchControlUseCase.BatchLaunchCommand.class);
        verify(batchControlUseCase, times(6)).launchSync(captor.capture());

        List<BatchControlUseCase.BatchLaunchCommand> commands = captor.getAllValues();
        assertThat(commands).extracting(BatchControlUseCase.BatchLaunchCommand::jobType)
                .containsExactly(
                        BatchControlUseCase.BatchJobType.STOCK_MASTER_SYNC,
                        BatchControlUseCase.BatchJobType.STOCK_PRICE_SYNC,
                        BatchControlUseCase.BatchJobType.BENCHMARK_PRICE_SYNC,
                        BatchControlUseCase.BatchJobType.STOCK_FOREIGN_INSTITUTION,
                        BatchControlUseCase.BatchJobType.SECTOR_EOD_SYNC,
                        BatchControlUseCase.BatchJobType.PORTFOLIO_STATS_SYNC
                );
        assertThat(commands).extracting(BatchControlUseCase.BatchLaunchCommand::publishEvent)
                .containsExactly(false, true, false, false, false, false);
        assertThat(commands.get(1).endDate()).isEqualTo(DateUtil.format(businessDate));
        assertThat(commands.get(2).startDate()).isEqualTo(DateUtil.format(businessDate.minusDays(7)));
        assertThat(commands.get(2).endDate()).isEqualTo(DateUtil.format(businessDate));
        assertThat(commands.get(3).startDate()).isEqualTo(DateUtil.format(businessDate));
    }

    @Test
    @DisplayName("중간 단계가 COMPLETED가 아니면 즉시 중단하고 예외를 던진다")
    void runDailyFullSync_stopsWhenAnyStepFails() {
        given(batchControlUseCase.launchSync(any()))
                .willReturn(completed("stockMasterSyncJob"))
                .willReturn(new BatchControlUseCase.BatchExecutionResult(2L, "stockPriceBatchJob", "FAILED", null, "failed"));

        assertThatThrownBy(() -> dailyBatchOrchestrationService.runDailyFullSync(LocalDate.of(2026, 4, 10)))
                .isInstanceOf(BatchException.class);

        verify(batchControlUseCase, times(2)).launchSync(any());
    }

    @Test
    @DisplayName("launchSync 예외는 배치 오케스트레이션 예외로 변환한다")
    void runDailyFullSync_wrapsLaunchException() {
        given(batchControlUseCase.launchSync(any()))
                .willThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> dailyBatchOrchestrationService.runDailyFullSync(LocalDate.of(2026, 4, 10)))
                .isInstanceOf(BatchException.class);
    }

    private BatchControlUseCase.BatchExecutionResult completed(String jobName) {
        return new BatchControlUseCase.BatchExecutionResult(1L, jobName, "COMPLETED", null, "ok");
    }
}
