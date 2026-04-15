package org.stockwellness.adapter.in.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.batch.BatchControlUseCase;

/**
 * 종목 마스터 동기화 Job 스케줄러.
 * 매일 오전 7시(KST) {@code stockMasterSyncJob}을 실행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockMasterSyncScheduler {

    private final BatchControlUseCase batchControlUseCase;

    @Scheduled(cron = "0 0 16 * * *", zone = "Asia/Seoul")
    public void schedule() {
        try {
            log.info("[StockMasterSyncScheduler] Job 기동");
            batchControlUseCase.launchSync(new BatchControlUseCase.BatchLaunchCommand(
                    BatchControlUseCase.BatchJobType.STOCK_MASTER_SYNC,
                    null,
                    null,
                    null,
                    false
            ));
        } catch (Exception e) {
            log.error("[StockMasterSyncScheduler] Job 기동 실패", e);
        }
    }
}
