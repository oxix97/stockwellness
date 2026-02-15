package org.stockwellness.batch.job.stock.price;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.fixture.StockFixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
class StockPriceIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @MockitoBean
    private KisDailyPriceAdapter kisDailyPriceAdapter;

    @Autowired
    @Qualifier("stockPriceBatchJob") // [중요] 여러 Job 중 이것을 주입받음
    private Job stockPriceBatchJob;

    @BeforeEach
    void setUp() {
        // [필수] 유틸리티가 어떤 Job을 실행할지 알려줘야 합니다.
        jobLauncherTestUtils.setJob(stockPriceBatchJob);

        stockPriceRepository.deleteAll();
        stockRepository.deleteAll();
    }

    @Test
    @DisplayName("JobParameter로 기간을 지정하여 삼성전자 시세를 수집한다")    void run_job_with_parameters() throws Exception {
        // Given
        Stock samsung = StockFixture.createSamsung();
        stockRepository.save(samsung);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("startDate", "20260101")
                .addString("endDate", "20260213")
                .addLong("time", System.currentTimeMillis()) // 중복 실행 방지용
                .toJobParameters();

        // When
        // JobLauncherTestUtils를 통해 Job 실행
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }
}