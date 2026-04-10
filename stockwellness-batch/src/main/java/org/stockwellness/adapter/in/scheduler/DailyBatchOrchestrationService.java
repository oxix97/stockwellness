package org.stockwellness.adapter.in.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.stereotype.Service;
import org.stockwellness.application.port.in.batch.BatchControlUseCase;
import org.stockwellness.batch.support.exception.BatchException;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.util.DateUtil;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyBatchOrchestrationService {

    private final BatchControlUseCase batchControlUseCase;

    private static final List<ScheduledBatchStep> DAILY_FULL_SYNC_STEPS = List.of(
            new ScheduledBatchStep(BatchControlUseCase.BatchJobType.STOCK_MASTER_SYNC, false, "[1단계] 종목 마스터 데이터 동기화 시작..."),
            new ScheduledBatchStep(BatchControlUseCase.BatchJobType.STOCK_PRICE_SYNC, true, "[2단계] 종목 시세 동기화 및 카프카 이벤트 발행 시작..."),
            new ScheduledBatchStep(BatchControlUseCase.BatchJobType.BENCHMARK_PRICE_SYNC, false, "[3단계] 지수(KOSPI·KOSDAQ·S&P500) 시세 동기화 시작..."),
            new ScheduledBatchStep(BatchControlUseCase.BatchJobType.STOCK_FOREIGN_INSTITUTION, false, "[4단계] 국내기관 및 외국인 매매종목 보정 시작..."),
            new ScheduledBatchStep(BatchControlUseCase.BatchJobType.SECTOR_EOD_SYNC, false, "[5단계] 섹터 인사이트 및 AI 분석 동기화 시작..."),
            new ScheduledBatchStep(BatchControlUseCase.BatchJobType.PORTFOLIO_STATS_SYNC, false, "[6단계] 전체 사용자 포트폴리오 성과 지표 갱신 시작...")
    );

    public void runDailyFullSync() {
        long timestamp = System.currentTimeMillis();
        log.info(">>> 일일 전체 데이터 동기화 배치 시작 [시작 시각: {}]", timestamp);
        
        LocalDate today = DateUtil.today();

        try {
            for (ScheduledBatchStep step : DAILY_FULL_SYNC_STEPS) {
                log.info(step.logLabel());
                
                // 지수 동기화의 경우 오늘 날짜를 명시적으로 전달하여 오늘 종가를 가져오도록 함
                BatchControlUseCase.BatchLaunchCommand command = (step.jobType() == BatchControlUseCase.BatchJobType.BENCHMARK_PRICE_SYNC)
                        ? step.toCommandWithDates(today.minusDays(7), today)
                        : step.toCommand();

                BatchControlUseCase.BatchExecutionResult result = batchControlUseCase.launchSync(command);
                validateStatus(step, result);
            }
            log.info(">>> 일일 전체 데이터 동기화 배치 성공적으로 완료.");
        } catch (BatchException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error(">>> 일일 전체 동기화 배치 중 심각한 오류 발생: {}", exception.getMessage(), exception);
            throw new BatchException(ErrorCode.BATCH_ORCHESTRATION_FAILED);
        }
    }

    private void validateStatus(
            ScheduledBatchStep step,
            BatchControlUseCase.BatchExecutionResult result
    ) {
        if (!BatchStatus.COMPLETED.name().equals(result.status())) {
            log.error(
                    "[Scheduler] 단계 실행 실패. step={}, jobName={}, status={}",
                    step.jobType().name(),
                    result.jobName(),
                    result.status()
            );
            throw new BatchException(ErrorCode.BATCH_ORCHESTRATION_FAILED);
        }
    }

    record ScheduledBatchStep(
            BatchControlUseCase.BatchJobType jobType,
            boolean publishEvent,
            String logLabel
    ) {
        BatchControlUseCase.BatchLaunchCommand toCommand() {
            return new BatchControlUseCase.BatchLaunchCommand(
                    jobType,
                    null,
                    null,
                    null,
                    publishEvent
            );
        }

        BatchControlUseCase.BatchLaunchCommand toCommandWithDates(LocalDate start, LocalDate end) {
            return new BatchControlUseCase.BatchLaunchCommand(
                    jobType,
                    null,
                    DateUtil.format(start),
                    DateUtil.format(end),
                    publishEvent
            );
        }
    }
}
