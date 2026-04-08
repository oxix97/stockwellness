package org.stockwellness.batch.support.operations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.application.port.in.batch.BatchControlUseCase;
import org.stockwellness.application.port.in.batch.BatchMonitoringUseCase;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.batch.job.stockmaster.application.MarketIndexSyncService;
import org.stockwellness.domain.stock.price.PriceIssueType;
import org.stockwellness.global.util.DateUtil;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchOperationsService implements BatchControlUseCase, BatchMonitoringUseCase {

    private static final LocalDate MIN_PRICE_SYNC_DATE = LocalDate.of(2022, 1, 1);

    @Qualifier("jobLauncher")
    private final JobLauncher jobLauncher;
    @Qualifier("asyncJobLauncher")
    private final JobLauncher asyncJobLauncher;
    private final JobExplorer jobExplorer;
    private final JobOperator jobOperator;
    private final Job stockMasterSyncJob;
    private final Job stockPriceBatchJob;
    private final Job sectorEodJob;
    private final Job stockPricePrevCloseSyncJob;
    private final Job portfolioStatsJob;
    private final Job benchmarkPriceSyncJob;
    private final Job stockInvestorTradeDetailJob;
    private final StockPort stockPort;
    private final MarketIndexSyncService marketIndexSyncService;
    private final StockPriceRepository stockPriceRepository;

    @Override
    public BatchExecutionResult launchAsync(BatchLaunchCommand command) {
        Job job = resolveJob(command.jobType());
        JobParameters parameters = buildParameters(command);
        return toResult(runJob(asyncJobLauncher, job, parameters));
    }

    @Override
    public BatchExecutionResult launchSync(BatchLaunchCommand command) {
        Job job = resolveJob(command.jobType());
        JobParameters parameters = buildParameters(command);
        return toResult(runJob(jobLauncher, job, parameters));
    }

    @Override
    public BatchExecutionResult syncIndices(MarketIndexSyncCommand command) {
        try {
            marketIndexSyncService.syncIndices(command.fileName());
            return new BatchExecutionResult(
                    0L,
                    BatchJobType.MARKET_INDEX_SYNC.jobName(),
                    "COMPLETED",
                    null,
                    "업종 마스터 동기화 성공"
            );
        } catch (Exception e) {
            log.error("업종 마스터 동기화 실패", e);
            throw new IllegalStateException("업종 마스터 동기화 실패", e);
        }
    }

    @Override
    public String stop(BatchStopCommand command) {
        try {
            boolean result = jobOperator.stop(command.executionId());
            return result ? "중단 요청 성공" : "중단 실패 (이미 종료되었을 수 있음)";
        } catch (Exception e) {
            return "오류 발생: " + e.getMessage();
        }
    }

    @Override
    public List<BatchJobStatusResult> getRecentJobStatuses(String jobName, int limit) {
        return jobExplorer.getJobInstances(jobName, 0, limit).stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .map(execution -> new BatchJobStatusResult(
                        execution.getId(),
                        execution.getJobInstance().getJobName(),
                        execution.getStatus().name(),
                        execution.getStartTime(),
                        execution.getEndTime(),
                        execution.getExitStatus().getExitCode()
                ))
                .sorted((left, right) -> right.executionId().compareTo(left.executionId()))
                .toList();
    }

    @Override
    public DataIntegrityResult checkDataIntegrity(LocalDate startDate, LocalDate endDate) {
        var invalidPrices = stockPriceRepository.findInvalidPrices(startDate, endDate);
        List<InvalidPriceIssue> issues = invalidPrices.stream()
                .map(price -> new InvalidPriceIssue(
                        price.getStock().getTicker(),
                        price.getStock().getName(),
                        price.getId().getBaseDate(),
                        price.getClosePrice() == null ? PriceIssueType.NULL_PRICE.name() : PriceIssueType.ZERO_PRICE.name()
                ))
                .toList();
        return new DataIntegrityResult(issues.size(), issues);
    }

    private Job resolveJob(BatchJobType jobType) {
        return switch (jobType) {
            case STOCK_MASTER_SYNC -> stockMasterSyncJob;
            case STOCK_PRICE_SYNC -> stockPriceBatchJob;
            case SECTOR_EOD_SYNC -> sectorEodJob;
            case STOCK_PRICE_PREV_CLOSE_SYNC -> stockPricePrevCloseSyncJob;
            case PORTFOLIO_STATS_SYNC -> portfolioStatsJob;
            case BENCHMARK_PRICE_SYNC -> benchmarkPriceSyncJob;
            case STOCK_FOREIGN_INSTITUTION -> stockInvestorTradeDetailJob;
            case MARKET_INDEX_SYNC -> throw new IllegalArgumentException("MARKET_INDEX_SYNC는 Job이 아닌 직접 동기화입니다.");
        };
    }

    private JobExecution runJob(JobLauncher launcher, Job job, JobParameters parameters) {
        try {
            return launcher.run(job, parameters);
        } catch (Exception e) {
            log.error("잡 실행 실패: {}", job.getName(), e);
            throw new IllegalStateException("배치 실행 실패: " + job.getName(), e);
        }
    }

    private JobParameters buildParameters(BatchLaunchCommand command) {
        validateTargetTicker(command);

        JobParametersBuilder builder = new JobParametersBuilder();
        switch (command.jobType()) {
            case STOCK_MASTER_SYNC, SECTOR_EOD_SYNC, STOCK_PRICE_SYNC, STOCK_PRICE_PREV_CLOSE_SYNC, PORTFOLIO_STATS_SYNC, STOCK_FOREIGN_INSTITUTION -> builder.addLong("time", System.currentTimeMillis());
            case BENCHMARK_PRICE_SYNC -> builder.addLong("timestamp", System.currentTimeMillis());
            case MARKET_INDEX_SYNC -> {
            }
        }

        if (command.jobType() == BatchJobType.STOCK_PRICE_SYNC || command.jobType() == BatchJobType.STOCK_PRICE_PREV_CLOSE_SYNC) {
            builder.addString("startDate", normalizePriceDate(command.startDate(), true));
            builder.addString("endDate", normalizePriceDate(command.endDate(), false));
        } else if (command.jobType() == BatchJobType.BENCHMARK_PRICE_SYNC) {
            builder.addString("startDate", normalizeDate(command.startDate()));
            builder.addString("endDate", normalizeDate(command.endDate()));
        } else if (command.jobType() == BatchJobType.STOCK_FOREIGN_INSTITUTION) {
            builder.addString("baseDate", normalizeDate(command.startDate()));
        } else {
            builder.addString("startDate", command.startDate());
            builder.addString("endDate", command.endDate());
        }

        if (command.targetTicker() != null) {
            builder.addString("targetTicker", command.targetTicker());
        }
        if (command.jobType() == BatchJobType.STOCK_PRICE_SYNC) {
            builder.addString("publishEvent", String.valueOf(command.publishEvent()));
        }
        return builder.toJobParameters();
    }

    private BatchExecutionResult toResult(JobExecution execution) {
        String jobName = execution.getJobInstance().getJobName();
        return new BatchExecutionResult(
                execution.getId(),
                jobName,
                execution.getStatus().name(),
                "/api/v1/admin/batch/status/" + jobName,
                String.format("배치 잡 [%s]이 시작되었습니다. (ExecutionId: %d)", jobName, execution.getId())
        );
    }

    private void validateTargetTicker(BatchLaunchCommand command) {
        if (command.targetTicker() == null || command.targetTicker().isBlank()) {
            return;
        }
        if ((command.jobType() == BatchJobType.STOCK_PRICE_PREV_CLOSE_SYNC
                || command.jobType() == BatchJobType.STOCK_FOREIGN_INSTITUTION)
                && !stockPort.existsByTicker(command.targetTicker())) {
            throw new IllegalArgumentException("존재하지 않는 종목입니다: " + command.targetTicker());
        }
    }

    private String normalizePriceDate(String rawDate, boolean applyLowerBound) {
        LocalDate date = parseFlexibleDate(rawDate);
        if (date == null) {
            return null;
        }
        if (applyLowerBound && date.isBefore(MIN_PRICE_SYNC_DATE)) {
            date = MIN_PRICE_SYNC_DATE;
        }
        return DateUtil.format(date);
    }

    private String normalizeDate(String rawDate) {
        LocalDate date = parseFlexibleDate(rawDate);
        return date != null ? DateUtil.format(date) : null;
    }

    private LocalDate parseFlexibleDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank() || "null".equalsIgnoreCase(rawDate)) {
            return null;
        }
        if (rawDate.contains("-")) {
            return LocalDate.parse(rawDate);
        }
        return DateUtil.parse(rawDate);
    }
}
