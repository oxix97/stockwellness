package org.stockwellness.adapter.in.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.stockwellness.application.service.portfolio.AdvisorOrchestrator;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockwellnessScheduler {

    private final DailyBatchOrchestrationService dailyBatchOrchestrationService;
    private final AdvisorOrchestrator advisorOrchestrator;

    /**
     * 매일 평일(월-금) 오후 3시 45분에 전체 데이터 동기화 배치를 실행합니다.
     */
    @Scheduled(cron = "0 45 15 * * MON-FRI", zone = "Asia/Seoul")
    public void runDailyFullSync() {
        dailyBatchOrchestrationService.runDailyFullSync();
    }

    /**
     * 매주 일요일 오후 6시에 모든 포트폴리오에 대한 AI 리밸런싱 조언을 생성합니다.
     */
    @Scheduled(cron = "0 0 18 * * SUN", zone = "Asia/Seoul")
    public void runAiAdvisorRebalancing() {
        log.info(">>> 주간 AI 어드바이저 리밸런싱 오케스트레이션 시작...");
        try {
            advisorOrchestrator.runAllPortfolios();
            log.info(">>> 주간 AI 어드바이저 리밸런싱 완료.");
        } catch (Exception e) {
            log.error(">>> AI 어드바이저 리밸런싱 실행 실패: {}", e.getMessage(), e);
        }
    }
}
