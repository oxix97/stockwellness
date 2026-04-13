package org.stockwellness.adapter.in.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.port.in.batch.BatchControlUseCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockInvestorTradeDetailSchedulerTest {

    @Mock
    private BatchControlUseCase batchControlUseCase;

    @InjectMocks
    private StockInvestorTradeDetailScheduler stockInvestorTradeDetailScheduler;

    @Test
    @DisplayName("외국인/기관 매매종목가 스케줄러는 STOCK_FOREIGN_INSTITUTION 배치를 동기 실행한다")
    void runStockInvestorTradeDetailJob_launchesForeignInstitutionBatch() {
        given(batchControlUseCase.launchSync(any()))
                .willReturn(new BatchControlUseCase.BatchExecutionResult(
                        1L,
                        "stockInvestorTradeDetailJob",
                        "COMPLETED",
                        null,
                        "ok"
                ));

        stockInvestorTradeDetailScheduler.runStockInvestorTradeDetailJob();

        ArgumentCaptor<BatchControlUseCase.BatchLaunchCommand> captor =
                ArgumentCaptor.forClass(BatchControlUseCase.BatchLaunchCommand.class);
        verify(batchControlUseCase).launchSync(captor.capture());

        BatchControlUseCase.BatchLaunchCommand command = captor.getValue();
        assertThat(command.jobType()).isEqualTo(BatchControlUseCase.BatchJobType.STOCK_FOREIGN_INSTITUTION);
        assertThat(command.targetTicker()).isNull();
        assertThat(command.startDate()).isNull();
        assertThat(command.endDate()).isNull();
        assertThat(command.publishEvent()).isFalse();
    }

    @Test
    @DisplayName("외국인/기관 매매종목가 스케줄러는 예외를 삼키고 종료한다")
    void runStockInvestorTradeDetailJob_swallowsException() {
        doThrow(new IllegalStateException("boom"))
                .when(batchControlUseCase)
                .launchSync(any());

        stockInvestorTradeDetailScheduler.runStockInvestorTradeDetailJob();

        verify(batchControlUseCase).launchSync(any());
    }
}
