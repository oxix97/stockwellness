package org.stockwellness.adapter.in.scheduler;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.TradingCalendarPort;
import org.stockwellness.application.service.marketdata.UsMarketCalendar;
import org.stockwellness.batch.support.exception.BatchException;
import org.stockwellness.domain.stock.TradingCalendar;
import org.stockwellness.global.error.ErrorCode;

/** Runs the canonical market-data jobs in market-session order. */
@Slf4j
@Component
public class MarketDataBatchOrchestrator {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final ZoneId US_ZONE = ZoneId.of("America/New_York");
    private static final int CALENDAR_LOOKBACK_DAYS = 14;
    private static final Pattern RUN_ID_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    private final JobLauncher jobLauncher;
    private final ApplicationContext applicationContext;
    private final TradingCalendarPort tradingCalendarPort;
    private final UsMarketCalendar usMarketCalendar;

    public MarketDataBatchOrchestrator(@Qualifier("jobLauncher") JobLauncher jobLauncher,
            ApplicationContext applicationContext, TradingCalendarPort tradingCalendarPort,
            UsMarketCalendar usMarketCalendar) {
        this.jobLauncher = jobLauncher;
        this.applicationContext = applicationContext;
        this.tradingCalendarPort = tradingCalendarPort;
        this.usMarketCalendar = usMarketCalendar;
    }

    public RunResult runScheduled(String marketCode) {
        return runScheduled(marketCode, Instant.now());
    }

    RunResult runScheduled(String marketCode, Instant now) {
        Market market = Market.parse(marketCode);
        if (now == null) throw new IllegalArgumentException("Current instant is required");
        String runId = scheduledRunId(market, now);
        List<StageResult> calendarStages = prepareRecentCalendar(market, now, runId);
        LocalDate latest = resolveLatestCompletedSession(market, now);
        RunResult result = runPrepared(market, latest, latest, latest, runId, calendarStages);
        log.info("Canonical market-data run completed: market={}, date={}, runId={}, stages={}",
                market, latest, runId, result.stages().size());
        return result;
    }

    public RunResult runManual(String marketCode, LocalDate startDate, LocalDate requestedEndDate, String requestedRunId) {
        return runManual(marketCode, startDate, requestedEndDate, requestedRunId, Instant.now());
    }

    RunResult runManual(String marketCode, LocalDate startDate, LocalDate requestedEndDate,
            String requestedRunId, Instant now) {
        Market market = Market.parse(marketCode);
        if (startDate == null || requestedEndDate == null || startDate.isAfter(requestedEndDate) || now == null) {
            throw new IllegalArgumentException("Market and a valid start/end date range are required");
        }
        String runId = normalizeRunId(requestedRunId, market, startDate, requestedEndDate);
        List<StageResult> calendarStages = new ArrayList<>();
        if (market == Market.KR) calendarStages.addAll(prepareRecentCalendar(market, now, runId));
        LocalDate latestCompleted = resolveLatestCompletedSession(market, now);
        LocalDate endLimit = requestedEndDate.isAfter(latestCompleted) ? latestCompleted : requestedEndDate;
        LocalDate effectiveEndDate;
        if (market == Market.US) {
            effectiveEndDate = usMarketCalendar.latestTradingDayOnOrBefore("NYSE", endLimit).getTradeDate();
        } else {
            calendarStages.addAll(prepareCalendarRange(market, endLimit.minusDays(CALENDAR_LOOKBACK_DAYS), endLimit, runId));
            effectiveEndDate = tradingCalendarPort.latestTradingDayOnOrBefore("KOSPI", endLimit)
                    .orElseThrow(() -> new IllegalStateException("No verified completed KOSPI session on or before " + endLimit));
        }
        if (startDate.isAfter(effectiveEndDate)) {
            throw new IllegalArgumentException("The requested range has no completed market session");
        }
        if (market == Market.US) {
            LocalDate today = now.atZone(US_ZONE).toLocalDate();
            LocalDate recentStart = today.minusDays(CALENDAR_LOOKBACK_DAYS);
            if (recentStart.getYear() != today.getYear()) recentStart = LocalDate.of(today.getYear(), 1, 1);
            if (startDate.isBefore(recentStart)) recentStart = startDate;
            calendarStages.addAll(prepareCalendarRange(market, recentStart, today, runId));
        } else {
            calendarStages.addAll(prepareCalendarRange(market, startDate, effectiveEndDate, runId));
        }
        RunResult result = runPrepared(market, startDate, requestedEndDate, effectiveEndDate, runId, calendarStages);
        log.info("Canonical market-data manual run completed: market={}, requestedRange={}..{}, effectiveEnd={}, runId={}, stages={}",
                market, startDate, requestedEndDate, effectiveEndDate, runId, result.stages().size());
        return result;
    }

    private List<StageResult> prepareRecentCalendar(Market market, Instant now, String runId) {
        if (now == null) throw new IllegalArgumentException("Current instant is required");
        ZoneId zone = market == Market.KR ? KOREA_ZONE : US_ZONE;
        LocalDate today = now.atZone(zone).toLocalDate();
        LocalDate start = today.minusDays(CALENDAR_LOOKBACK_DAYS);
        if (market == Market.US && start.getYear() != today.getYear()) {
            start = LocalDate.of(today.getYear(), 1, 1);
        }
        return prepareCalendarRange(market, start, today, runId);
    }

    private List<StageResult> prepareCalendarRange(Market market, LocalDate start, LocalDate end, String runId) {
        if (market == Market.US) {
            List<TradingCalendar> rows = new ArrayList<>();
            rows.addAll(usMarketCalendar.calendarDays("NYSE", start, end));
            rows.addAll(usMarketCalendar.calendarDays("NASDAQ", start, end));
            tradingCalendarPort.upsert(rows);
            return List.of(new StageResult("calendar-" + start + "-" + end,
                    "officialUsMarketCalendar", "COMPLETED", null));
        }

        List<StageResult> stages = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            TradingCalendar kospi = tradingCalendarPort.find("KOSPI", date).orElse(null);
            TradingCalendar kosdaq = tradingCalendarPort.find("KOSDAQ", date).orElse(null);
            if (calendarPairComplete(kospi, kosdaq)) continue;
            if (date.getDayOfWeek().getValue() > 5) {
                tradingCalendarPort.upsert(List.of(
                        TradingCalendar.of("KOSPI", date, false, null, null, "KRX:WEEKEND"),
                        TradingCalendar.of("KOSDAQ", date, false, null, null, "KRX:WEEKEND")));
                stages.add(new StageResult("calendar-" + date, "krxWeekendCalendar", "COMPLETED", null));
                continue;
            }
            JobParameters parameters = new JobParametersBuilder()
                    .addString("baseDate", date.toString())
                    .addString("runId", runId + "-calendar-" + date)
                    .toJobParameters();
            stages.add(runJob("calendar-" + date, "kisTradingCalendarSyncJob", parameters));
        }
        return List.copyOf(stages);
    }

    private static boolean calendarPairComplete(TradingCalendar kospi, TradingCalendar kosdaq) {
        if (kospi == null || kosdaq == null || kospi.isTradingDay() != kosdaq.isTradingDay()) return false;
        if (!kospi.isTradingDay()) return true;
        return kospi.getOpensAt() != null && kospi.getClosesAt() != null
                && kospi.getOpensAt().equals(kosdaq.getOpensAt())
                && kospi.getClosesAt().equals(kosdaq.getClosesAt());
    }

    private LocalDate resolveLatestCompletedSession(Market market, Instant now) {
        if (market == Market.US) return usMarketCalendar.latestCompletedSession("NYSE", now).getTradeDate();

        LocalDate today = now.atZone(KOREA_ZONE).toLocalDate();
        TradingCalendar todayCalendar = tradingCalendarPort.find("KOSPI", today)
                .orElseThrow(() -> new IllegalStateException("KOSPI calendar is unknown for " + today));
        if (todayCalendar.isTradingDay()) {
            if (todayCalendar.getClosesAt() == null) {
                throw new IllegalStateException("KOSPI session close time is unknown for " + today);
            }
            if (!now.isBefore(todayCalendar.getClosesAt().toInstant())) return today;
        }
        LocalDate previous = tradingCalendarPort.latestTradingDayOnOrBefore("KOSPI", today.minusDays(1))
                .orElseThrow(() -> new IllegalStateException("No verified completed KOSPI session before " + today));
        return previous;
    }

    private RunResult runPrepared(Market market, LocalDate startDate, LocalDate requestedEndDate,
            LocalDate effectiveEndDate, String runId, List<StageResult> calendarStages) {
        List<StageResult> stages = new ArrayList<>(calendarStages);
        if (market == Market.KR) {
            stages.add(runStage(market, startDate, requestedEndDate, effectiveEndDate, runId,
                    "stockMasterSyncJob"));
            stages.add(runStage(market, startDate, requestedEndDate, effectiveEndDate, runId,
                    "kisEodStockPriceSyncJob"));
            stages.add(runStage(market, startDate, requestedEndDate, effectiveEndDate, runId,
                    "kisEodInvestorTradeSyncJob"));
        }
        stages.add(runStage(market, startDate, requestedEndDate, effectiveEndDate, runId,
                "kisEodIndexPriceSyncJob"));
        stages.add(runStage(market, startDate, requestedEndDate, effectiveEndDate, runId,
                "marketDerivedDataJob"));
        return new RunResult(market.name(), startDate, requestedEndDate, effectiveEndDate, runId, List.copyOf(stages));
    }

    private StageResult runStage(Market market, LocalDate startDate, LocalDate requestedEndDate,
            LocalDate effectiveEndDate, String runId, String jobName) {
        JobParameters parameters = new JobParametersBuilder()
                .addString("market", market.name())
                .addString("startDate", startDate.toString())
                .addString("endDate", effectiveEndDate.toString())
                .addString("requestedEndDate", requestedEndDate.toString())
                .addString("targetDate", effectiveEndDate.toString())
                .addString("sessionBasis", "REGULAR")
                .addString("runId", runId)
                .toJobParameters();
        return runJob(jobName, jobName, parameters);
    }

    private StageResult runJob(String stage, String jobName, JobParameters parameters) {
        try {
            Job job = applicationContext.getBean(jobName, Job.class);
            JobExecution execution = jobLauncher.run(job, parameters);
            if (execution.getStatus() != BatchStatus.COMPLETED) {
                log.error("Canonical market-data stage failed: stage={}, job={}, status={}",
                        stage, jobName, execution.getStatus());
                throw new BatchException(ErrorCode.BATCH_ORCHESTRATION_FAILED);
            }
            return new StageResult(stage, jobName, execution.getStatus().name(), execution.getId());
        } catch (JobInstanceAlreadyCompleteException alreadyCompleted) {
            log.info("Canonical market-data stage already completed: stage={}, job={}", stage, jobName);
            return new StageResult(stage, jobName, "ALREADY_COMPLETED", null);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobParametersInvalidException exception) {
            log.error("Canonical market-data stage could not be launched: stage={}, job={}", stage, jobName, exception);
            throw new BatchException(ErrorCode.BATCH_ORCHESTRATION_FAILED);
        } catch (BatchException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("Canonical market-data stage failed to resolve or execute: stage={}, job={}", stage, jobName, exception);
            throw new BatchException(ErrorCode.BATCH_ORCHESTRATION_FAILED);
        }
    }

    private static String scheduledRunId(Market market, Instant now) {
        ZoneId zone = market == Market.KR ? KOREA_ZONE : US_ZONE;
        return market.name() + "-daily-" + now.atZone(zone).toLocalDate();
    }

    private static String normalizeRunId(String requestedRunId, Market market, LocalDate start, LocalDate end) {
        String runId = requestedRunId == null || requestedRunId.isBlank()
                ? market.name() + "-" + start + "-" + end
                : requestedRunId;
        if (!RUN_ID_PATTERN.matcher(runId).matches()) {
            throw new IllegalArgumentException("runId must contain 1-100 letters, digits, dots, underscores, or hyphens");
        }
        return runId;
    }

    enum Market {
        KR, US;

        static Market parse(String value) {
            try {
                return Market.valueOf(value == null ? "" : value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("market must be KR or US", exception);
            }
        }
    }

    public record StageResult(String stage, String jobName, String status, Long executionId) {}

    public record RunResult(String market, LocalDate startDate, LocalDate requestedEndDate,
            LocalDate effectiveEndDate, String runId, List<StageResult> stages) {}
}
