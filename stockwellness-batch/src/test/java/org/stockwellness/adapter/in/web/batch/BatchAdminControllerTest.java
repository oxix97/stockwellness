package org.stockwellness.adapter.in.web.batch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.stockwellness.application.port.in.batch.BatchControlUseCase;
import org.stockwellness.application.port.in.batch.BatchMonitoringUseCase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchAdminController.class)
class BatchAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BatchControlUseCase batchControlUseCase;

    @MockitoBean
    private BatchMonitoringUseCase batchMonitoringUseCase;

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
    void testIntegrityCheckResponseFormat() throws Exception {
        when(batchMonitoringUseCase.checkDataIntegrity(any(), any())).thenReturn(
                new BatchMonitoringUseCase.DataIntegrityResult(0, java.util.List.of())
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
                        && "005930".equals(command.targetTicker())
                        && "20260408".equals(command.startDate())
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "baseDate": "20260408",
                                  "targetTicker": "005930"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.executionId").value(456))
                .andExpect(jsonPath("$.data.jobName").value("stockInvestorTradeDetailJob"))
                .andExpect(jsonPath("$.data.statusUrl").value("/api/v1/admin/batch/status/stockInvestorTradeDetailJob"))
                .andExpect(jsonPath("$.data.message").exists());
    }
}
