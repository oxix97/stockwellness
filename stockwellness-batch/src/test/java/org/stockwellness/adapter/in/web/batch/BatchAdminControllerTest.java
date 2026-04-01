package org.stockwellness.adapter.in.web.batch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchAdminController.class)
@AutoConfigureMockMvc(addFilters = false) // Security 필터 비활성화 (테스트 편의성)
class BatchAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobLauncher jobLauncher;

    @MockBean(name = "benchmarkPriceSyncJob")
    private Job benchmarkPriceSyncJob;

    @Test
    @DisplayName("배치 수동 실행 API 성공 테스트")
    void syncBenchmarkPrice_success() throws Exception {
        mockMvc.perform(post("/api/v1/admin/batch/benchmark-sync")
                        .param("startDate", "2026-03-01"))
                .andExpect(status().isOk())
                .andExpect(content().string("Benchmark price sync job started successfully"));

        verify(jobLauncher).run(any(), any());
    }

    @Test
    @DisplayName("배치 수동 실행 API 실패 테스트 - 잘못된 날짜 형식")
    void syncBenchmarkPrice_invalidDate() throws Exception {
        mockMvc.perform(post("/api/v1/admin/batch/benchmark-sync")
                        .param("startDate", "20260401")) // 하이픈 없음
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid startDate format. Use yyyy-MM-dd"));
    }
}
