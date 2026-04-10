package org.stockwellness.batch.job.investortradedetail.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.InvestorTradeDetail;
import org.stockwellness.adapter.out.persistence.stock.StockAdapter;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.fixture.StockFixture;
import org.stockwellness.support.BatchIntegrationTestSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("StockInvestorTradeDetailJob 통합 테스트")
class StockInvestorTradeDetailJobIntegrationTest extends BatchIntegrationTestSupport {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("stockInvestorTradeDetailJob")
    private Job job;

    @Autowired
    private StockAdapter stockAdapter;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @MockitoBean
    private KisDailyPriceAdapter kisDailyPriceAdapter;

    @Test
    @DisplayName("지정 기준일의 stock_price 수급 컬럼만 보정하고 중복 티커는 순매수 상위 응답을 우선한다")
    void stockInvestorTradeDetailJob_updatesInvestorAmounts() throws Exception {
        Stock samsung = stockAdapter.save(StockFixture.createSamsung());
        LocalDate baseDate = LocalDate.of(2026, 4, 8);
        stockPriceRepository.save(StockPrice.of(
                samsung,
                baseDate,
                new BigDecimal("70000"),
                new BigDecimal("71000"),
                new BigDecimal("69000"),
                new BigDecimal("70500"),
                new BigDecimal("70500"),
                new BigDecimal("70000"),
                1_000_000L,
                new BigDecimal("70500000000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null
        ));

        given(kisDailyPriceAdapter.fetchForeignInstitutionData("0001", "0"))
                .willReturn(List.of(investorTradeDetail("005930", "15", "7")));
        given(kisDailyPriceAdapter.fetchForeignInstitutionData("0001", "1"))
                .willReturn(List.of(investorTradeDetail("005930", "999", "999")));
        given(kisDailyPriceAdapter.fetchForeignInstitutionData("1001", "0"))
                .willReturn(Collections.emptyList());
        given(kisDailyPriceAdapter.fetchForeignInstitutionData("1001", "1"))
                .willReturn(Collections.emptyList());

        JobExecution jobExecution = jobLauncher.run(job, new JobParametersBuilder()
                .addString("baseDate", "20260408")
                .addLong("time", System.currentTimeMillis())
                .toJobParameters());

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StockPrice updated = stockPriceRepository
                .findByStockTickerInAndIdBaseDateBetween(List.of("005930"), baseDate, baseDate)
                .getFirst();

        assertThat(updated.getNetInstitutionalBuyingQty()).isEqualTo(100L);
        assertThat(updated.getNetForeignBuyingQty()).isEqualTo(200L);
        assertThat(updated.getNetInstitutionalBuyingAmt()).isEqualByComparingTo("15000000");
        assertThat(updated.getNetForeignBuyingAmt()).isEqualByComparingTo("7000000");

        verify(kisDailyPriceAdapter).fetchForeignInstitutionData("0001", "0");
        verify(kisDailyPriceAdapter).fetchForeignInstitutionData("0001", "1");
        verify(kisDailyPriceAdapter).fetchForeignInstitutionData("1001", "0");
        verify(kisDailyPriceAdapter).fetchForeignInstitutionData("1001", "1");
        verifyNoMoreInteractions(kisDailyPriceAdapter);
    }

    private InvestorTradeDetail investorTradeDetail(String ticker, String institutionalAmount, String foreignAmount) {
        return new InvestorTradeDetail(
                "테스트",
                ticker,
                null,
                null,
                null,
                null,
                null,
                null,
                "200",
                "100",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                foreignAmount,
                institutionalAmount,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
