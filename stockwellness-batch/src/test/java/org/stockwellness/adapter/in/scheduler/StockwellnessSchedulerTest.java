package org.stockwellness.adapter.in.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.application.service.portfolio.AdvisorOrchestrator;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockwellnessSchedulerTest {

    @Mock
    private DailyBatchOrchestrator dailyBatchOrchestrator;

    @Mock
    private AdvisorOrchestrator advisorOrchestrator;

    @InjectMocks
    private StockwellnessScheduler stockwellnessScheduler;

    @Test
    @DisplayName("일일 전체 동기화 스케줄러는 오케스트레이션 서비스만 호출한다")
    void runDailyFullSync_delegatesToOrchestrator() {
        stockwellnessScheduler.runDailyFullSync();

        verify(dailyBatchOrchestrator).runDailyStockSync();
        verify(dailyBatchOrchestrator).runDailySectorInsightSync();
        verify(dailyBatchOrchestrator).runDailyMarketSync();
    }

    @Test
    @DisplayName("주간 AI 리밸런싱은 오케스트레이터 실행 후 종료한다")
    void runAiAdvisorRebalancing_runsAdvisorOrchestrator() {
        stockwellnessScheduler.runAiAdvisorRebalancing();

        verify(advisorOrchestrator).runAllPortfolios();
    }

    @Test
    @DisplayName("주간 AI 리밸런싱 실패는 로그만 남기고 예외를 다시 던지지 않는다")
    void runAiAdvisorRebalancing_swallowsException() {
        doThrow(new IllegalStateException("boom"))
                .when(advisorOrchestrator)
                .runAllPortfolios();

        stockwellnessScheduler.runAiAdvisorRebalancing();

        verify(advisorOrchestrator).runAllPortfolios();
    }
}
