package org.stockwellness.adapter.in.web.batch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.stockwellness.batch.exception.BatchException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BenchmarkController.class)
@AutoConfigureMockMvc(addFilters = false) // Security 필터 비활성화 (테스트 편의성)
class BenchmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobLauncher jobLauncher;

    @MockitoBean(name = "benchmarkPriceSyncJob")
    private Job benchmarkPriceSyncJob;

    @Test
    @DisplayName("배치 수동 실행 API 성공 테스트")
    void syncBenchmarkPrice_success() throws Exception {
        mockMvc.perform(post("/api/v1/admin/batch/benchmark-sync")
                        .param("startDate", "20260301"))
                .andExpect(status().isAccepted())
                .andExpect(content().string("지수 시세 동기화 배치가 비동기로 시작되었습니다. 진행 상황은 로그를 확인하세요."));

        verify(jobLauncher).run(any(), any());
    }

    @Test
    @DisplayName("배치 수동 실행 API 실패 테스트 - 잘못된 날짜 형식")
    void syncBenchmarkPrice_invalidDate() {
        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/api/v1/admin/batch/benchmark-sync")
                    .param("startDate", "2026-03-01")); // 하이픈 포함 (이제는 실패해야 함)
        });
    }
}
