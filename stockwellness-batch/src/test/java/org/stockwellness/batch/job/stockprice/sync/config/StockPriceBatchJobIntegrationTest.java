package org.stockwellness.batch.job.stockprice.sync.config;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail;
import org.stockwellness.adapter.out.external.kis.dto.KisInvestorPriceDetail;
import org.stockwellness.adapter.out.persistence.stock.StockAdapter;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.fixture.StockFixture;
import org.stockwellness.support.BatchIntegrationTestSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@DisplayName("StockPriceBatchJob 통합 테스트")
class StockPriceBatchJobIntegrationTest extends BatchIntegrationTestSupport {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("stockPriceBatchJob")
    private Job job;

    @Autowired
    private StockAdapter stockAdapter;

    @MockitoBean
    private KisDailyPriceAdapter kisDailyPriceAdapter;

    @Test
    @DisplayName("주식 시세 수집 배치가 성공적으로 실행된다 (Gap Sync 흐름)")
    void stockPriceBatchJob_Success() throws Exception {
        // 준비
        Stock samsung = StockFixture.createSamsung();
        stockAdapter.save(samsung);

        // 일별 시세(과거 이력)에 대한 Mock API 응답 설정
        KisDailyPriceDetail dailyDetail = new KisDailyPriceDetail(
                LocalDate.of(2024, 1, 1),
                new BigDecimal("70000"), new BigDecimal("71000"), new BigDecimal("69000"), new BigDecimal("70500"),
                1000000L, new BigDecimal("70500000000"),
                "00", "1.0", "N", "2", new BigDecimal("500"), "00"
        );
        given(kisDailyPriceAdapter.fetchDailyPrices(any(Stock.class), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of(dailyDetail));

        // 투자자 수급 데이터에 대한 Mock API 응답 설정
        KisInvestorPriceDetail investorDetail = new KisInvestorPriceDetail(
                "20240101",
                new BigDecimal("70500"), "2", new BigDecimal("500"), new BigDecimal("0.71"),
                1000000L, 5000L, 20000L, 10000L, 
                new BigDecimal("350000000"), new BigDecimal("1400000000"), new BigDecimal("700000000")
        );
        given(kisDailyPriceAdapter.fetchInvestorPrices(any(Stock.class), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of(investorDetail));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("startDate", "20240101")
                .addString("endDate", "20240101")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // 실행
        JobExecution jobExecution = jobLauncher.run(job, jobParameters);

        // 검증
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
