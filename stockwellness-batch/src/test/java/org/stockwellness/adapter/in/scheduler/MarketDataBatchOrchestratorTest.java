package org.stockwellness.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.stockwellness.batch.support.exception.BatchException;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.domain.stock.TradingCalendar;
import org.stockwellness.domain.stock.Currency;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockSector;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.application.port.out.stock.TradingCalendarPort;
import org.stockwellness.application.service.marketdata.UsMarketCalendar;

@ExtendWith(MockitoExtension.class)
class MarketDataBatchOrchestratorTest {

    @Mock JobLauncher jobLauncher;
    @Mock ApplicationContext applicationContext;
    @Mock TradingCalendarPort calendarPort;
    @Mock StockPort stockPort;
    private MarketDataBatchOrchestrator orchestrator;

    @BeforeEach
    void setUp() throws Exception {
        orchestrator = new MarketDataBatchOrchestrator(
                jobLauncher, applicationContext, calendarPort, new UsMarketCalendar(), stockPort);
        for (String jobName : List.of("kisTradingCalendarSyncJob", "stockMasterSyncJob",
                "kisEodStockPriceSyncJob", "kisEodInvestorTradeSyncJob",
                "kisEodIndexPriceSyncJob", "marketDerivedDataJob")) {
            Job job = mock(Job.class);
            lenient().when(job.getName()).thenReturn(jobName);
            lenient().when(applicationContext.getBean(jobName, Job.class)).thenReturn(job);
        }
        lenient().when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            JobParameters parameters = invocation.getArgument(1);
            JobExecution execution = new JobExecution(new JobInstance(1L, job.getName()), parameters);
            execution.setStatus(BatchStatus.COMPLETED);
            return execution;
        });
    }

    @Test
    void us_manual_run_clamps_end_to_last_completed_early_close_and_runs_only_us_index_jobs() throws Exception {
        var result = orchestrator.runManual("US", LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 27), null,
                Instant.parse("2026-11-27T17:59:59Z"));

        assertThat(result.effectiveEndDate()).isEqualTo(LocalDate.of(2026, 11, 25));
        assertThat(result.stages()).extracting(MarketDataBatchOrchestrator.StageResult::jobName)
                .containsExactly("officialUsMarketCalendar", "kisEodIndexPriceSyncJob", "marketDerivedDataJob");
        assertThat(result.stages()).allSatisfy(stage -> assertThat(stage.status()).isEqualTo("COMPLETED"));
        verify(applicationContext, never()).getBean(eq("stockMasterSyncJob"), eq(Job.class));
        verify(applicationContext, never()).getBean(eq("kisEodStockPriceSyncJob"), eq(Job.class));
        verify(applicationContext, never()).getBean(eq("kisEodInvestorTradeSyncJob"), eq(Job.class));
        verify(calendarPort).upsert(any());
        ArgumentCaptor<JobParameters> parameters = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher, times(2)).run(any(Job.class), parameters.capture());
        assertThat(parameters.getAllValues()).allSatisfy(jobParameters -> {
            assertThat(jobParameters.getString("market")).isEqualTo("US");
            assertThat(jobParameters.getString("endDate")).isEqualTo("2026-11-25");
            assertThat(jobParameters.getString("sessionBasis")).isEqualTo("REGULAR");
        });
    }

    @Test
    void us_manual_run_does_not_move_end_date_past_the_last_completed_session() throws Exception {
        var result = orchestrator.runManual("US", LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 1), null,
                Instant.parse("2026-11-27T18:00:00Z"));

        assertThat(result.effectiveEndDate()).isEqualTo(LocalDate.of(2026, 11, 27));
        verify(jobLauncher).run(eq(job("kisEodIndexPriceSyncJob")), any());
    }

    @Test
    void us_manual_run_moves_a_holiday_end_date_back_to_the_previous_trading_session() {
        var result = orchestrator.runManual("US", LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 26), null,
                Instant.parse("2026-11-30T22:00:00Z"));

        assertThat(result.effectiveEndDate()).isEqualTo(LocalDate.of(2026, 11, 25));
    }

    @Test
    void scheduled_us_run_uses_market_local_run_id_and_the_completed_regular_session() throws Exception {
        var result = orchestrator.runScheduled("US", Instant.parse("2026-01-05T21:15:00Z"));

        assertThat(result.effectiveEndDate()).isEqualTo(LocalDate.of(2026, 1, 5));
        assertThat(result.runId()).isEqualTo("US-daily-2026-01-05");
        assertThat(result.stages()).extracting(MarketDataBatchOrchestrator.StageResult::jobName)
                .containsExactly("officialUsMarketCalendar", "kisEodIndexPriceSyncJob", "marketDerivedDataJob");
        ArgumentCaptor<JobParameters> parameters = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher, times(2)).run(any(Job.class), parameters.capture());
        assertThat(parameters.getAllValues()).allSatisfy(jobParameters -> {
            assertThat(jobParameters.getString("market")).isEqualTo("US");
            assertThat(jobParameters.getString("runId")).isEqualTo("US-daily-2026-01-05");
            assertThat(jobParameters.getString("sessionBasis")).isEqualTo("REGULAR");
        });
    }

    @Test
    void scheduled_us_calendar_range_starts_at_january_first_when_lookback_crosses_year_boundary() {
        var result = orchestrator.runScheduled("US", Instant.parse("2026-01-05T21:15:00Z"));

        assertThat(result.stages()).extracting(MarketDataBatchOrchestrator.StageResult::stage)
                .contains("calendar-2026-01-01-2026-01-05");
    }

    @Test
    @SuppressWarnings("unchecked")
    void scheduled_us_run_at_2027_year_boundary_prepares_prior_year_completed_session_calendar() {
        var result = orchestrator.runScheduled("US", Instant.parse("2027-01-04T20:59:59Z"));

        assertThat(result.effectiveEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(result.stages()).extracting(MarketDataBatchOrchestrator.StageResult::stage)
                .contains("calendar-2026-12-31-2027-01-04");
        ArgumentCaptor<List<TradingCalendar>> upserted = ArgumentCaptor.forClass(List.class);
        verify(calendarPort).upsert(upserted.capture());
        assertThat(upserted.getValue())
                .anySatisfy(day -> {
                    assertThat(day.getMarketCode()).isEqualTo("NYSE");
                    assertThat(day.getTradeDate()).isEqualTo(LocalDate.of(2026, 12, 31));
                    assertThat(day.isTradingDay()).isTrue();
                })
                .anySatisfy(day -> {
                    assertThat(day.getMarketCode()).isEqualTo("NASDAQ");
                    assertThat(day.getTradeDate()).isEqualTo(LocalDate.of(2026, 12, 31));
                    assertThat(day.isTradingDay()).isTrue();
                });
    }

    @Test
    @SuppressWarnings("unchecked")
    void scheduled_us_new_years_day_and_manual_year_boundary_use_2026_12_31_and_prepare_calendar() throws Exception {
        var scheduledHoliday = orchestrator.runScheduled("US", Instant.parse("2027-01-01T21:15:00Z"));
        assertThat(scheduledHoliday.effectiveEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(scheduledHoliday.runId()).isEqualTo("US-daily-2027-01-01");
        assertThat(scheduledHoliday.stages()).extracting(MarketDataBatchOrchestrator.StageResult::stage)
                .contains("calendar-2026-12-31-2027-01-01");

        var manualBoundary = orchestrator.runManual("US", LocalDate.of(2026, 12, 31),
                LocalDate.of(2027, 1, 4), "us-year-boundary", Instant.parse("2027-01-04T20:59:59Z"));
        assertThat(manualBoundary.effectiveEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(manualBoundary.stages()).extracting(MarketDataBatchOrchestrator.StageResult::stage)
                .contains("calendar-2026-12-31-2027-01-04");

        ArgumentCaptor<List<TradingCalendar>> upserted = ArgumentCaptor.forClass(List.class);
        verify(calendarPort, times(2)).upsert(upserted.capture());
        assertThat(upserted.getAllValues().get(0)).anySatisfy(day -> {
            assertThat(day.getMarketCode()).isEqualTo("NYSE");
            assertThat(day.getTradeDate()).isEqualTo(LocalDate.of(2027, 1, 1));
            assertThat(day.isTradingDay()).isFalse();
        });
    }

    @Test
    void kr_run_orders_calendar_master_price_investor_index_and_derived_jobs() {
        LocalDate today = LocalDate.of(2026, 11, 15);
        LocalDate previousSession = LocalDate.of(2026, 11, 13);
        given(calendarPort.find(eq("KOSPI"), any(LocalDate.class))).willAnswer(invocation ->
                java.util.Optional.of(calendarDay("KOSPI", invocation.getArgument(1))));
        given(calendarPort.find(eq("KOSDAQ"), any(LocalDate.class))).willAnswer(invocation ->
                java.util.Optional.of(calendarDay("KOSDAQ", invocation.getArgument(1))));
        given(calendarPort.latestTradingDayOnOrBefore("KOSPI", today.minusDays(1)))
                .willReturn(java.util.Optional.of(previousSession));
        given(calendarPort.latestTradingDayOnOrBefore("KOSPI", previousSession))
                .willReturn(java.util.Optional.of(previousSession));

        var result = orchestrator.runManual("KR", previousSession, today,
                null, Instant.parse("2026-11-15T03:00:00Z"));

        assertThat(result.effectiveEndDate()).isEqualTo(previousSession);
        assertThat(result.stages()).extracting(MarketDataBatchOrchestrator.StageResult::jobName)
                .containsExactly("stockMasterSyncJob", "kisEodStockPriceSyncJob", "kisEodInvestorTradeSyncJob",
                        "kisEodIndexPriceSyncJob", "marketDerivedDataJob");
    }

    @Test
    void targeted_kr_manual_run_passes_ticker_and_skips_unscoped_master_and_derived_jobs() throws Exception {
        LocalDate today = LocalDate.of(2026, 11, 15);
        LocalDate previousSession = LocalDate.of(2026, 11, 13);
        given(calendarPort.find(eq("KOSPI"), any(LocalDate.class))).willAnswer(invocation ->
                java.util.Optional.of(calendarDay("KOSPI", invocation.getArgument(1))));
        given(calendarPort.find(eq("KOSDAQ"), any(LocalDate.class))).willAnswer(invocation ->
                java.util.Optional.of(calendarDay("KOSDAQ", invocation.getArgument(1))));
        given(calendarPort.latestTradingDayOnOrBefore("KOSPI", today.minusDays(1)))
                .willReturn(java.util.Optional.of(previousSession));
        given(calendarPort.latestTradingDayOnOrBefore("KOSPI", previousSession))
                .willReturn(java.util.Optional.of(previousSession));
        given(stockPort.loadStocksByTickers(List.of("005930"))).willReturn(List.of(
                Stock.of("005930", "KR7005930003", "삼성전자", MarketType.KOSPI, Currency.KRW,
                        StockSector.empty(), StockStatus.ACTIVE)));

        var result = orchestrator.runManual("KR", previousSession, today, "kis-252-sample", "005930",
                Instant.parse("2026-11-15T03:00:00Z"));

        assertThat(result.stages()).extracting(MarketDataBatchOrchestrator.StageResult::jobName)
                .containsExactly("kisEodStockPriceSyncJob", "kisEodInvestorTradeSyncJob", "kisEodIndexPriceSyncJob");
        verify(applicationContext, never()).getBean(eq("stockMasterSyncJob"), eq(Job.class));
        verify(applicationContext, never()).getBean(eq("marketDerivedDataJob"), eq(Job.class));
        ArgumentCaptor<Job> jobs = ArgumentCaptor.forClass(Job.class);
        ArgumentCaptor<JobParameters> parameters = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher, times(3)).run(jobs.capture(), parameters.capture());
        for (int i = 0; i < jobs.getAllValues().size(); i++) {
            String jobName = jobs.getAllValues().get(i).getName();
            String ticker = parameters.getAllValues().get(i).getString("ticker");
            if (jobName.equals("kisEodStockPriceSyncJob") || jobName.equals("kisEodInvestorTradeSyncJob")) {
                assertThat(ticker).isEqualTo("005930");
            } else {
                assertThat(jobName).isEqualTo("kisEodIndexPriceSyncJob");
                assertThat(ticker).isNull();
            }
        }
    }

    @Test
    void ticker_filter_is_rejected_for_us_manual_runs() {
        assertThatThrownBy(() -> orchestrator.runManual("US", LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 27), "kis-252-sample", "005930",
                Instant.parse("2026-11-27T18:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("KRX");
    }

    @Test
    void targeted_kr_manual_run_rejects_a_ticker_missing_from_the_krx_master_before_side_effects() throws Exception {
        given(stockPort.loadStocksByTickers(List.of("999999"))).willReturn(List.of());

        assertThatThrownBy(() -> orchestrator.runManual("KR", LocalDate.of(2026, 11, 13),
                LocalDate.of(2026, 11, 13), "kis-252-sample", "999999",
                Instant.parse("2026-11-15T03:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stock master");

        verify(calendarPort, never()).find(any(), any());
        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    void scheduled_kr_run_fills_missing_weekdays_from_kis_and_marks_weekends_without_provider_calls() throws Exception {
        LocalDate today = LocalDate.of(2026, 11, 15);
        LocalDate previousSession = LocalDate.of(2026, 11, 13);
        given(calendarPort.find(any(), any(LocalDate.class))).willAnswer(invocation -> {
            String marketCode = invocation.getArgument(0);
            LocalDate date = invocation.getArgument(1);
            return today.equals(date)
                    ? java.util.Optional.of(TradingCalendar.of(marketCode, date, false, null, null, "test"))
                    : java.util.Optional.empty();
        });
        given(calendarPort.latestTradingDayOnOrBefore("KOSPI", today.minusDays(1)))
                .willReturn(java.util.Optional.of(previousSession));

        var result = orchestrator.runScheduled("KR", Instant.parse("2026-11-15T03:00:00Z"));

        assertThat(result.effectiveEndDate()).isEqualTo(previousSession);
        assertThat(result.runId()).isEqualTo("KR-daily-2026-11-15");
        assertThat(result.stages()).extracting(MarketDataBatchOrchestrator.StageResult::stage)
                .contains("calendar-2026-11-07", "calendar-2026-11-08");
        assertThat(result.stages().stream().filter(stage -> stage.jobName().equals("kisTradingCalendarSyncJob")))
                .hasSize(10);
        assertThat(result.stages()).extracting(MarketDataBatchOrchestrator.StageResult::jobName)
                .contains("stockMasterSyncJob", "kisEodStockPriceSyncJob", "kisEodInvestorTradeSyncJob");
    }

    @Test
    void scheduled_kr_run_fails_closed_when_today_is_open_but_session_close_is_unknown() {
        LocalDate today = LocalDate.of(2026, 11, 19);
        given(calendarPort.find(eq("KOSPI"), any(LocalDate.class))).willAnswer(invocation -> {
            LocalDate date = invocation.getArgument(1);
            return java.util.Optional.of(date.equals(today)
                    ? TradingCalendar.of("KOSPI", date, true, null, null, "test")
                    : calendarDay("KOSPI", date));
        });
        given(calendarPort.find(eq("KOSDAQ"), any(LocalDate.class))).willAnswer(invocation -> {
            LocalDate date = invocation.getArgument(1);
            return java.util.Optional.of(date.equals(today)
                    ? TradingCalendar.of("KOSDAQ", date, true, null, null, "test")
                    : calendarDay("KOSDAQ", date));
        });

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> orchestrator.runScheduled("KR",
                        Instant.parse("2026-11-19T07:00:00Z")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("session close time is unknown");
        verify(applicationContext, never()).getBean(eq("stockMasterSyncJob"), eq(Job.class));
    }

    @Test
    void failed_source_job_stops_before_derived_data_and_is_reported_as_batch_failure() throws Exception {
        Job indexJob = job("kisEodIndexPriceSyncJob");
        given(jobLauncher.run(eq(indexJob), any())).willAnswer(invocation -> {
            JobExecution failed = new JobExecution(new JobInstance(2L, indexJob.getName()), invocation.getArgument(1));
            failed.setStatus(BatchStatus.FAILED);
            return failed;
        });

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> orchestrator.runManual("US",
                        LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 25), "failed-source",
                        Instant.parse("2026-11-27T18:00:00Z")))
                .isInstanceOf(BatchException.class);

        verify(applicationContext, never()).getBean(eq("marketDerivedDataJob"), eq(Job.class));
    }

    @Test
    void already_completed_source_stage_is_reported_and_later_stages_continue() throws Exception {
        Job indexJob = job("kisEodIndexPriceSyncJob");
        given(jobLauncher.run(eq(indexJob), any()))
                .willThrow(new JobInstanceAlreadyCompleteException("same source window already completed"));

        var result = orchestrator.runManual("US", LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 25),
                "same-window", Instant.parse("2026-11-27T18:00:00Z"));

        assertThat(result.stages()).extracting(MarketDataBatchOrchestrator.StageResult::status)
                .containsExactly("COMPLETED", "ALREADY_COMPLETED", "COMPLETED");
        verify(applicationContext).getBean("marketDerivedDataJob", Job.class);
    }

    @Test
    void scheduled_run_rejects_missing_current_instant_before_starting_jobs() throws Exception {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> orchestrator.runScheduled("KR", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Current instant is required");
        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
        verify(calendarPort, never()).find(any(), any());
    }

    @Test
    void invalid_market_dates_and_run_ids_are_rejected_before_job_launch() throws Exception {
        Instant afterUsClose = Instant.parse("2026-11-27T21:15:00Z");
        assertThatThrownBy(() -> orchestrator.runManual("OTHER", LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 27), null, afterUsClose))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("market must be KR or US");
        assertThatThrownBy(() -> orchestrator.runManual("US", null, LocalDate.of(2026, 11, 27), null, afterUsClose))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid start/end date range");
        assertThatThrownBy(() -> orchestrator.runManual("US", LocalDate.of(2026, 11, 28),
                LocalDate.of(2026, 11, 28), null, afterUsClose))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no completed market session");
        assertThatThrownBy(() -> orchestrator.runManual("US", LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 27), "bad/run/id", afterUsClose))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("runId");
        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    void job_launcher_conflicts_are_reported_as_batch_orchestration_failure() throws Exception {
        Job indexJob = job("kisEodIndexPriceSyncJob");
        given(jobLauncher.run(eq(indexJob), any()))
                .willThrow(new JobExecutionAlreadyRunningException("index sync already running"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> orchestrator.runManual("US",
                        LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 25), "conflicting-run",
                        Instant.parse("2026-11-27T21:15:00Z")))
                .isInstanceOf(BatchException.class);

        verify(applicationContext, never()).getBean(eq("marketDerivedDataJob"), eq(Job.class));
    }

    private static TradingCalendar calendarDay(String marketCode, LocalDate date) {
        boolean open = date.getDayOfWeek().getValue() <= 5;
        if (!open) return TradingCalendar.of(marketCode, date, false, null, null, "test");
        var openAt = date.atTime(LocalTime.of(9, 0)).atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
        var closeAt = date.atTime(LocalTime.of(15, 30)).atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
        return TradingCalendar.of(marketCode, date, true, openAt, closeAt, "test");
    }

    private Job job(String jobName) {
        return applicationContext.getBean(jobName, Job.class);
    }
}
