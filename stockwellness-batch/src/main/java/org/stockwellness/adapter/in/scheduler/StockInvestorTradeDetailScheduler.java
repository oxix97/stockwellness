package org.stockwellness.adapter.in.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.batch.BatchControlUseCase;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockInvestorTradeDetailScheduler {

    private final BatchControlUseCase batchControlUseCase;

    /**
     * 매주 평일(월-금) 오후 3시(KST) 외국인/기관 매매종목가 집계 배치를 실행합니다.
     */
    @Scheduled(cron = "0 0 15 * * MON-FRI", zone = "Asia/Seoul")
    public void runStockInvestorTradeDetailJob() {
        log.info("[Scheduler] 외국인/기관 매매종목가 집계 배치 자동 실행 시작");
        try {
            batchControlUseCase.launchSync(new BatchControlUseCase.BatchLaunchCommand(
                    BatchControlUseCase.BatchJobType.STOCK_FOREIGN_INSTITUTION,
                    null,
                    null,
                    null,
                    false
            ));
            log.info("[Scheduler] 외국인/기관 매매종목가 집계 배치 트리거 성공");
        } catch (Exception exception) {
            log.error("[Scheduler] 외국인/기관 매매종목가 집계 배치 실행 중 오류 발생", exception);
        }
    }
}
