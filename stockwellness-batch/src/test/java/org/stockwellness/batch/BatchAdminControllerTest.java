package org.stockwellness.batch;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.batch.job.stock.master.MarketIndexSyncService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchAdminController.class)
class BatchAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "asyncJobLauncher")
    private JobLauncher jobLauncher;

    @MockitoBean(name = "stockMasterSyncJob")
    private Job stockMasterSyncJob;

    @MockitoBean(name = "stockPriceBatchJob")
    private Job stockPriceBatchJob;

    @MockitoBean(name = "sectorEodJob")
    private Job sectorEodJob;

    @MockitoBean(name = "stockPricePrevCloseSyncJob")
    private Job stockPricePrevCloseSyncJob;

    @MockitoBean(name = "portfolioStatsJob")
    private Job portfolioStatsJob;

    @MockitoBean
    private StockPort stockPort;

    @MockitoBean
    private MarketIndexSyncService marketIndexSyncService;

    @MockitoBean
    private org.springframework.batch.core.explore.JobExplorer jobExplorer;

    @MockitoBean
    private JobOperator jobOperator;

    @MockitoBean
    private StockPriceRepository stockPriceRepository;

    @Test
    void testSyncMasterResponseFormat() throws Exception {
        // given
        JobInstance jobInstance = new JobInstance(1L, "stockMasterSyncJob");
        JobExecution jobExecution = new JobExecution(jobInstance, 123L, null);
        when(jobLauncher.run(any(), any())).thenReturn(jobExecution);
        when(stockMasterSyncJob.getName()).thenReturn("stockMasterSyncJob");

        // when & then
        mockMvc.perform(post("/api/v1/admin/batch/sync-master")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(123))
                .andExpect(jsonPath("$.jobName").value("stockMasterSyncJob"))
                .andExpect(jsonPath("$.statusUrl").value("/api/v1/admin/batch/status/stockMasterSyncJob"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testIntegrityCheckResponseFormat() throws Exception {
        // given
        when(stockPriceRepository.findInvalidPrices(any(), any())).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/admin/batch/integrity-check")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-02")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.issues").isArray());
    }
}
