package org.stockwellness.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;
import org.stockwellness.application.service.portfolio.AdvisorOrchestrator;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockwellnessSchedulerTest {

    @Mock
    private MarketDataBatchOrchestrator marketDataBatchOrchestrator;

    @Mock
    private AdvisorOrchestrator advisorOrchestrator;

    @InjectMocks
    private StockwellnessScheduler stockwellnessScheduler;

    @Test
    @DisplayName("일일 전체 동기화 스케줄러는 오케스트레이션 서비스만 호출한다")
    void runDailyFullSync_delegates_to_the_canonical_kr_orchestrator() {
        stockwellnessScheduler.runDailyFullSync();

        verify(marketDataBatchOrchestrator).runScheduled("KR");
    }

    @Test
    void kr_market_schedule_runs_after_the_verified_late_close_session() throws Exception {
        Scheduled schedule = StockwellnessScheduler.class.getMethod("runDailyFullSync")
                .getAnnotation(Scheduled.class);

        assertThat(schedule.cron()).isEqualTo("0 45 16 * * MON-FRI");
        assertThat(schedule.zone()).isEqualTo("Asia/Seoul");
    }

    @Test
    @DisplayName("미국 시장 스케줄은 해외 지수 전용 오케스트레이션을 호출한다")
    void runDailyUsMarketSync_delegates_to_the_us_orchestrator() {
        stockwellnessScheduler.runDailyUsMarketSync();

        verify(marketDataBatchOrchestrator).runScheduled("US");
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
