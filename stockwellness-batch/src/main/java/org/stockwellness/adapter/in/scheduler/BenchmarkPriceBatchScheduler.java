package org.stockwellness.adapter.in.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.batch.BatchControlUseCase;
import org.stockwellness.batch.support.exception.BatchException;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.util.DateUtil;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class BenchmarkPriceBatchScheduler {

    private final BatchControlUseCase batchControlUseCase;
    private static final int COLLECTION_LOOKBACK_DAYS = 14;

    /**
     * 매일 오전 8시(KST) 지수 시세 동기화 배치 실행
     * 미국 시장 마감(오전 6~7시) 후 수집하기 적절한 시간
     */
    @Scheduled(cron = "0 0 8 * * MON-FRI")
    public void runBenchmarkPriceSyncJob() {
        log.info("[Scheduler] 지수 시세 동기화 배치 자동 실행 시작");
        LocalDate syncEndDate = DateUtil.previousBusinessDay(LocalDate.now());
        LocalDate syncStartDate = syncEndDate.minusDays(COLLECTION_LOOKBACK_DAYS);
        try {
            BatchControlUseCase.BatchExecutionResult result = batchControlUseCase.launchSync(new BatchControlUseCase.BatchLaunchCommand(
                    BatchControlUseCase.BatchJobType.BENCHMARK_PRICE_SYNC,
                    null,
                    DateUtil.format(syncStartDate),
                    DateUtil.format(syncEndDate),
                    false
            ));
            validateStatus(result);
            log.info("[Scheduler] 지수 시세 동기화 배치 트리거 성공");
        } catch (Exception e) {
            log.error("[Scheduler] 배치 실행 중 오류 발생", e);
        }
    }

    private void validateStatus(BatchControlUseCase.BatchExecutionResult result) {
        if (!"COMPLETED".equals(result.status())) {
            throw new BatchException(ErrorCode.BATCH_EXECUTION_FAILED);
        }
    }
}
