package org.stockwellness.adapter.in.web.batch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.stockwellness.adapter.in.scheduler.DailyBatchOrchestrationService;
import org.stockwellness.adapter.in.scheduler.MarketDataBatchOrchestrator;
import org.stockwellness.adapter.in.web.batch.MarketDataSyncTokenVerifier;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.KisMultiStockPriceDetail;
import org.stockwellness.application.port.in.batch.BatchControlUseCase;
import org.stockwellness.application.port.in.batch.BatchMonitoringUseCase;
import org.stockwellness.batch.support.exception.BatchException;
import org.stockwellness.global.error.ErrorCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchAdminController.class)
@TestPropertySource(properties = "stockwellness.batch.market-data.sync-token=test-market-data-token")
@Import(MarketDataSyncTokenVerifier.class)
class BatchAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BatchControlUseCase batchControlUseCase;

    @MockitoBean
    private BatchMonitoringUseCase batchMonitoringUseCase;

    @MockitoBean
    private DailyBatchOrchestrationService dailyBatchOrchestrationService;

    @MockitoBean
    private MarketDataBatchOrchestrator marketDataBatchOrchestrator;

    @MockitoBean
    private KisDailyPriceAdapter kisDailyPriceAdapter;

    @Test
    void testFetchMultiPricesResponseFormat() throws Exception {
        KisMultiStockPriceDetail detail = new KisMultiStockPriceDetail(
                "005930", "삼성전자", new BigDecimal("70000"), new BigDecimal("1000"), new BigDecimal("1.45"),
                new BigDecimal("69000"), new BigDecimal("71000"), new BigDecimal("68000"), 1000000L, new BigDecimal("70000000000"),
                new BigDecimal("69000"), new BigDecimal("500000000"), new BigDecimal("300000000")
        );
        when(kisDailyPriceAdapter.fetchMultiStockPrices(List.of("005930")))
                .thenReturn(List.of(detail));

        mockMvc.perform(get("/api/v1/admin/batch/fetch-multi-prices")
                        .param("tickers", "005930")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].inter_shrn_iscd").value("005930"))
                .andExpect(jsonPath("$.data[0].inter_kor_isnm").value("삼성전자"));
    }

    @Test
    void testSyncMasterResponseFormat() throws Exception {
        when(batchControlUseCase.launchAsync(any())).thenReturn(
                new BatchControlUseCase.BatchExecutionResult(
                        123L,
                        "stockMasterSyncJob",
                        "STARTING",
                        "/api/v1/admin/batch/status/stockMasterSyncJob",
                        "배치 잡 [stockMasterSyncJob]이 시작되었습니다. (ExecutionId: 123)"
                )
        );

        mockMvc.perform(post("/api/v1/admin/batch/sync-master")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.executionId").value(123))
                .andExpect(jsonPath("$.data.jobName").value("stockMasterSyncJob"))
                .andExpect(jsonPath("$.data.statusUrl").value("/api/v1/admin/batch/status/stockMasterSyncJob"))
                .andExpect(jsonPath("$.data.message").exists());
    }

    @Test
    void marketDataSyncRequiresExplicitMarketAndDateRange() throws Exception {
        var result = new MarketDataBatchOrchestrator.RunResult("US", LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 27), LocalDate.of(2026, 11, 25), "us-2026-11-20-2026-11-27",
                List.of(new MarketDataBatchOrchestrator.StageResult(
                        "kisEodIndexPriceSyncJob", "kisEodIndexPriceSyncJob", "COMPLETED", 42L)));
        when(marketDataBatchOrchestrator.runManual("US", LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 27), "us-2026-11-20-2026-11-27")).thenReturn(result);

        mockMvc.perform(post("/api/v1/admin/batch/market-data/sync")
                        .header("X-Market-Data-Token", "test-market-data-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"market":"US","startDate":"2026-11-20","endDate":"2026-11-27",
                                 "runId":"us-2026-11-20-2026-11-27"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.market").value("US"))
                .andExpect(jsonPath("$.data.startDate").value("2026-11-20"))
                .andExpect(jsonPath("$.data.effectiveEndDate").value("2026-11-25"))
                .andExpect(jsonPath("$.data.stages[0].executionId").value(42));
    }

    @Test
    void runDailySyncUsesCanonicalMarketDataPipeline() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 16);
        var krResult = new MarketDataBatchOrchestrator.RunResult("KR", date, date, date, "scheduled-kr-20261116",
                List.of(new MarketDataBatchOrchestrator.StageResult(
                        "kisEodIndexPriceSyncJob", "kisEodIndexPriceSyncJob", "COMPLETED", 44L)));
        var usResult = new MarketDataBatchOrchestrator.RunResult("US", date, date, date, "scheduled-us-20261116",
                List.of(new MarketDataBatchOrchestrator.StageResult(
                        "kisEodIndexPriceSyncJob", "kisEodIndexPriceSyncJob", "COMPLETED", 45L)));
        when(marketDataBatchOrchestrator.runScheduled("KR")).thenReturn(krResult);
        when(marketDataBatchOrchestrator.runScheduled("US")).thenReturn(usResult);

        var response = mockMvc.perform(post("/api/v1/admin/batch/run-daily-sync")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(marketDataBatchOrchestrator);
        order.verify(marketDataBatchOrchestrator).runScheduled("KR");
        order.verify(marketDataBatchOrchestrator).runScheduled("US");
        response.andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].market").value("KR"))
                .andExpect(jsonPath("$.data[0].stages[0].jobName").value("kisEodIndexPriceSyncJob"))
                .andExpect(jsonPath("$.data[1].market").value("US"))
                .andExpect(jsonPath("$.data[1].stages[0].jobName").value("kisEodIndexPriceSyncJob"));
    }

    @Test
    void marketDataSyncPassesOptionalTickerForScopedKrxReconciliation() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 12);
        var result = new MarketDataBatchOrchestrator.RunResult("KR", date, date, date, "kis-252-sample",
                List.of(new MarketDataBatchOrchestrator.StageResult(
                        "kisEodStockPriceSyncJob", "kisEodStockPriceSyncJob", "COMPLETED", 43L)));
        when(marketDataBatchOrchestrator.runManual("KR", date, date, "kis-252-sample", "005930"))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/admin/batch/market-data/sync")
                        .header("X-Market-Data-Token", "test-market-data-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"market":"KR","startDate":"2026-11-12","endDate":"2026-11-12",
                                 "runId":"kis-252-sample","ticker":"005930"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.market").value("KR"))
                .andExpect(jsonPath("$.data.stages[0].executionId").value(43));

        verify(marketDataBatchOrchestrator).runManual("KR", date, date, "kis-252-sample", "005930");
    }

    @Test
    void marketDataSyncRejectsTickerFilterOutsideKrx() throws Exception {
        mockMvc.perform(post("/api/v1/admin/batch/market-data/sync")
                        .header("X-Market-Data-Token", "test-market-data-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"market":"US","startDate":"2026-11-20","endDate":"2026-11-27",
                                 "ticker":"005930"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        org.mockito.Mockito.verifyNoInteractions(marketDataBatchOrchestrator);
    }

    @Test
    void marketDataSyncRejectsMissingToken() throws Exception {
        mockMvc.perform(post("/api/v1/admin/batch/market-data/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"market":"US","startDate":"2026-11-20","endDate":"2026-11-27"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));

        org.mockito.Mockito.verifyNoInteractions(marketDataBatchOrchestrator);
    }

    @Test
    void marketDataSyncRejectsIncorrectToken() throws Exception {
        mockMvc.perform(post("/api/v1/admin/batch/market-data/sync")
                        .header("X-Market-Data-Token", "incorrect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"market":"US","startDate":"2026-11-20","endDate":"2026-11-27"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));

        org.mockito.Mockito.verifyNoInteractions(marketDataBatchOrchestrator);
    }

    @Test
    void testIntegrityCheckResponseFormat() throws Exception {
        when(batchMonitoringUseCase.checkDataIntegrity(any(), any())).thenReturn(
                new BatchMonitoringUseCase.DataIntegrityResult(0, List.of())
        );

        mockMvc.perform(get("/api/v1/admin/batch/integrity-check")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-02")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.issues").isArray());
    }

    @Test
    void testInvestorTradeDetailSyncResponseFormat() throws Exception {
        when(batchControlUseCase.launchAsync(argThat(command ->
                command.jobType() == BatchControlUseCase.BatchJobType.STOCK_FOREIGN_INSTITUTION
                        && command.targetTicker() == null
                        && command.startDate() == null
        ))).thenReturn(
                new BatchControlUseCase.BatchExecutionResult(
                        456L,
                        "stockInvestorTradeDetailJob",
                        "STARTING",
                        "/api/v1/admin/batch/status/stockInvestorTradeDetailJob",
                        "배치 잡 [stockInvestorTradeDetailJob]이 시작되었습니다. (ExecutionId: 456)"
                )
        );

        mockMvc.perform(post("/api/v1/admin/batch/sync-investor-trade-detail")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.executionId").value(456))
                .andExpect(jsonPath("$.data.jobName").value("stockInvestorTradeDetailJob"))
                .andExpect(jsonPath("$.data.statusUrl").value("/api/v1/admin/batch/status/stockInvestorTradeDetailJob"))
                .andExpect(jsonPath("$.data.message").exists());
    }

    @Test
    void runMarketWeatherBackfillHasNoDateParameters() throws Exception {
        when(batchControlUseCase.launchAsync(argThat(command ->
                command.jobType() == BatchControlUseCase.BatchJobType.MARKET_WEATHER_BACKFILL
                        && command.targetTicker() == null
                        && command.startDate() == null
                        && command.endDate() == null
                        && command.targetDate() == null
                        && !command.publishEvent()
        ))).thenReturn(
                new BatchControlUseCase.BatchExecutionResult(
                        789L,
                        "backfillMarketWeatherJob",
                        "STARTING",
                        "/api/v1/admin/batch/status/backfillMarketWeatherJob",
                        "배치 잡 [backfillMarketWeatherJob]이 시작되었습니다. (ExecutionId: 789)"
                )
        );

        mockMvc.perform(post("/api/v1/admin/batch/backfill-market-weather")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.executionId").value(789))
                .andExpect(jsonPath("$.data.jobName").value("backfillMarketWeatherJob"))
                .andExpect(jsonPath("$.data.statusUrl")
                        .value("/api/v1/admin/batch/status/backfillMarketWeatherJob"));
    }

    @Test
    void testRunDailyFullSyncResponseFormat() throws Exception {
        mockMvc.perform(post("/api/v1/admin/batch/run-daily-full-sync")
                        .content("""
                                {
                                  "endDate": "20260410"
                                }
                                """)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("S000"))
                .andExpect(jsonPath("$.message").value("요청이 성공적으로 처리되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(dailyBatchOrchestrationService).runDailyFullSync(LocalDate.of(2026, 4, 10));
    }

    @Test
    void testRunDailyFullSyncWithoutBodyUsesDefaultDate() throws Exception {
        mockMvc.perform(post("/api/v1/admin/batch/run-daily-full-sync")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(dailyBatchOrchestrationService).runDailyFullSync(null);
    }

    @Test
    void testRunDailyFullSyncFailureResponseFormat() throws Exception {
        doThrow(new BatchException(ErrorCode.BATCH_ORCHESTRATION_FAILED))
                .when(dailyBatchOrchestrationService)
                .runDailyFullSync(null);

        mockMvc.perform(post("/api/v1/admin/batch/run-daily-full-sync")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.BATCH_ORCHESTRATION_FAILED.getCode()));
    }

    @Test
    void runSinglePriceFetchWithoutTickerReturnsInvalidInputResponse() throws Exception {
        mockMvc.perform(post("/api/v1/admin/batch/fetch-prices/single")
                        .content("""
                                {
                                  "startDate": "20260410",
                                  "endDate": "20260410"
                                }
                                """)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
    }
}
