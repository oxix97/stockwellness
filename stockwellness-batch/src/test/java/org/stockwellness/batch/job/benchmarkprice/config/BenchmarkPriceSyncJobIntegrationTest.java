package org.stockwellness.batch.job.benchmarkprice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.BenchmarkPriceData;
import org.stockwellness.support.BatchIntegrationTestSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.batch.core.launch.JobLauncher;

@DisplayName("BenchmarkPriceSyncJob 통합 테스트")
class BenchmarkPriceSyncJobIntegrationTest extends BatchIntegrationTestSupport {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("benchmarkPriceSyncJob")
    private Job job;

    @MockitoBean
    private KisDailyPriceAdapter kisDailyPriceAdapter;

    @Test
    @DisplayName("지수 시세 동기화 배치가 성공적으로 실행된다")
    void benchmarkPriceSyncJob_Success() throws Exception {
        // 준비
        String startDate = "20240101";
        String endDate = "20240102";
        
        // 인터페이스 Mockito를 사용하여 모든 지수 유형에 대한 Mock API 응답 설정
        BenchmarkPriceData mockData = mock(BenchmarkPriceData.class);
        given(mockData.baseDate()).willReturn(LocalDate.of(2024, 1, 1));
        given(mockData.closePrice()).willReturn(new BigDecimal("2500.00"));
        given(mockData.prdyVrss()).willReturn(new BigDecimal("10.00"));
        given(mockData.prdyCtrt()).willReturn(new BigDecimal("0.4"));
        given(mockData.volume()).willReturn(1000000L);
        
        given(kisDailyPriceAdapter.fetchIndexDailyPrices(anyString(), any(), any()))
                .willReturn(List.of(mockData));
        given(kisDailyPriceAdapter.fetchOverseasIndexDailyPrices(anyString(), any(), any()))
                .willReturn(List.of(mockData));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("startDate", startDate)
                .addString("endDate", endDate)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // 실행
        JobExecution jobExecution = jobLauncher.run(job, jobParameters);

        // 검증
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    @DisplayName("일부 지수 조회가 실패해도 다른 지수 데이터가 있으면 배치는 완료된다")
    void benchmarkPriceSyncJob_CompletesWhenSomeBenchmarksFail() throws Exception {
        String startDate = "20240101";
        String endDate = "20240102";

        BenchmarkPriceData mockData = mock(BenchmarkPriceData.class);
        given(mockData.baseDate()).willReturn(LocalDate.of(2024, 1, 1));
        given(mockData.closePrice()).willReturn(new BigDecimal("5300.00"));
        given(mockData.prdyVrss()).willReturn(new BigDecimal("15.00"));
        given(mockData.prdyCtrt()).willReturn(new BigDecimal("0.3"));
        given(mockData.volume()).willReturn(500000L);

        given(kisDailyPriceAdapter.fetchIndexDailyPrices(anyString(), any(), any()))
                .willThrow(new IllegalStateException("국내 지수 응답 오류"));
        given(kisDailyPriceAdapter.fetchOverseasIndexDailyPrices(anyString(), any(), any()))
                .willReturn(List.of(mockData));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("startDate", startDate)
                .addString("endDate", endDate)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncher.run(job, jobParameters);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
