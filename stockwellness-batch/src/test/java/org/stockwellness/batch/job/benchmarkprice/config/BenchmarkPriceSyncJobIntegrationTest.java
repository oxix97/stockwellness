package org.stockwellness.batch.job.benchmarkprice.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.BenchmarkPriceData;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.support.BatchIntegrationTestSupport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("BenchmarkPriceSyncJob 통합 테스트")
class BenchmarkPriceSyncJobIntegrationTest extends BatchIntegrationTestSupport {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("benchmarkPriceSyncJob")
    private Job job;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private KisDailyPriceAdapter kisDailyPriceAdapter;

    @Test
    @DisplayName("지수 시세 동기화 배치가 성공적으로 실행된다")
    void benchmarkPriceSyncJob_Success() throws Exception {
        // 준비
        String startDate = "20240101";
        String endDate = "20240102";
        
        // 인터페이스 Mockito를 사용하여 모든 지수 유형에 대한 Mock API 응답 설정
        BenchmarkPriceData mockData = mockData(LocalDate.of(2024, 1, 1), "2500.00", "0.4", 1000000L);
        
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

        BenchmarkPriceData mockData = mockData(LocalDate.of(2024, 1, 1), "5300.00", "0.3", 500000L);

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

    @Test
    @DisplayName("ISO 날짜 파라미터도 허용한다")
    void benchmarkPriceSyncJob_acceptsIsoDateParameters() throws Exception {
        BenchmarkPriceData mockData = mockData(LocalDate.of(2024, 1, 1), "2500.00", "0.4", 1000000L);

        given(kisDailyPriceAdapter.fetchIndexDailyPrices(anyString(), any(), any()))
                .willReturn(List.of(mockData));
        given(kisDailyPriceAdapter.fetchOverseasIndexDailyPrices(anyString(), any(), any()))
                .willReturn(List.of(mockData));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("startDate", "2024-01-01")
                .addString("endDate", "2024-01-02")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncher.run(job, jobParameters);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    @DisplayName("같은 Job 재실행 시 이전 실행의 미래 종가를 과거 기준일에 사용하지 않는다")
    void benchmarkPriceSyncJob_rerunDoesNotReuseFutureCloseFromPreviousExecution() throws Exception {
        LocalDate firstDate = LocalDate.of(2024, 1, 1);
        LocalDate secondDate = LocalDate.of(2024, 1, 2);
        BenchmarkPriceData first = mockData(firstDate, "100", "0", 100L);
        BenchmarkPriceData second = mockData(secondDate, "110", "0", 100L);
        given(kisDailyPriceAdapter.fetchIndexDailyPrices(anyString(), any(), any())).willReturn(List.of(first, second));
        given(kisDailyPriceAdapter.fetchOverseasIndexDailyPrices(anyString(), any(), any())).willReturn(List.of(first, second));
        long timestamp = System.currentTimeMillis();

        JobExecution firstRun = jobLauncher.run(job, jobParameters(timestamp));
        assertThat(firstRun.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(changeRate(BenchmarkType.KOSPI_200.getTicker(), secondDate)).isEqualByComparingTo("10");

        JobExecution rerun = jobLauncher.run(job, jobParameters(timestamp + 1));

        assertThat(rerun.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(changeRate(BenchmarkType.KOSPI_200.getTicker(), firstDate)).isEqualByComparingTo("0");
    }

    private JobParameters jobParameters(long timestamp) {
        return new JobParametersBuilder()
                .addString("startDate", "20240101")
                .addString("endDate", "20240102")
                .addLong("timestamp", timestamp)
                .toJobParameters();
    }

    private BigDecimal changeRate(String ticker, LocalDate baseDate) {
        return jdbcTemplate.queryForObject(
                "select change_rate from benchmark_price where ticker = ? and base_date = ?",
                BigDecimal.class,
                ticker,
                baseDate
        );
    }

    private BenchmarkPriceData mockData(LocalDate date, String close, String changeRate, long volume) {
        BenchmarkPriceData data = mock(BenchmarkPriceData.class);
        given(data.baseDate()).willReturn(date);
        given(data.openPrice()).willReturn(new BigDecimal(close).subtract(BigDecimal.ONE));
        given(data.highPrice()).willReturn(new BigDecimal(close).add(BigDecimal.ONE));
        given(data.lowPrice()).willReturn(new BigDecimal(close).subtract(BigDecimal.TEN));
        given(data.closePrice()).willReturn(new BigDecimal(close));
        given(data.prdyVrss()).willReturn(BigDecimal.ZERO);
        given(data.prdyCtrt()).willReturn(new BigDecimal(changeRate));
        given(data.volume()).willReturn(volume);
        return data;
    }
}
