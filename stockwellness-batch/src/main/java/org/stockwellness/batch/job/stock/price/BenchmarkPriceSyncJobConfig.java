package org.stockwellness.batch.job.stock.price;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.BenchmarkPriceData;
import org.stockwellness.application.port.out.stock.BenchmarkPricePort;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.domain.stock.price.BenchmarkPrice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BenchmarkPriceSyncJobConfig {

    private final KisDailyPriceAdapter kisAdapter;
    private final BenchmarkPricePort benchmarkPricePort;

    @Bean
    public Job benchmarkPriceSyncJob(JobRepository jobRepository, Step benchmarkPriceSyncStep) {
        return new JobBuilder("benchmarkPriceSyncJob", jobRepository)
                .start(benchmarkPriceSyncStep)
                .build();
    }

    @Bean
    public Step benchmarkPriceSyncStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("benchmarkPriceSyncStep", jobRepository)
                .tasklet(benchmarkPriceSyncTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet benchmarkPriceSyncTasklet() {
        return (contribution, chunkContext) -> {
            LocalDate today = LocalDate.now();
            // 파라미터가 없으면 2년 전부터 수집 (초기 데이터 구축용)
            String startDateParam = (String) chunkContext.getStepContext().getJobParameters().get("startDate");
            LocalDate startDate = startDateParam != null ? LocalDate.parse(startDateParam) : today.minusYears(2);

            for (BenchmarkType type : BenchmarkType.values()) {
                String systemTicker = type.getTicker();
                String apiTicker = type.getApiTicker();
                log.info("[Benchmark] 시세 수집 시작: {} (System: {}, API: {}, StartDate: {})", 
                        type.getDescription(), systemTicker, apiTicker, startDate);

                try {
                    List<BenchmarkPriceData> details;
                    if (type.isOverseas()) {
                        details = kisAdapter.fetchOverseasIndexDailyPrices(apiTicker, startDate);
                    } else {
                        details = kisAdapter.fetchIndexDailyPrices(apiTicker, startDate);
                    }

                    int count = 0;
                    for (BenchmarkPriceData detail : details) {
                        saveBenchmarkPrice(systemTicker, detail);
                        count++;
                    }
                    log.info("[Benchmark] 시세 수집 완료: {} ({}건 저장/업데이트)", type.getDescription(), count);
                } catch (Exception e) {
                    log.error("[Benchmark] 시세 수집 중 오류 발생: {} ({}) - {}", 
                            type.getDescription(), systemTicker, e.getMessage());
                }
            }
            return RepeatStatus.FINISHED;
        };
    }

    private void saveBenchmarkPrice(String ticker, BenchmarkPriceData detail) {
        LocalDate date = detail.baseDate();
        if (date == null) return;

        final BigDecimal close = detail.closePrice();
        if (close == null || close.compareTo(BigDecimal.ZERO) <= 0) return;

        final BigDecimal changeRate = calculateChangeRate(detail, close);

        benchmarkPricePort.findByTickerAndBaseDate(ticker, date)
                .ifPresentOrElse(
                    existing -> existing.updatePrices(
                        detail.openPrice(), detail.highPrice(), detail.lowPrice(), 
                        close, changeRate, detail.volume()
                    ),
                    () -> {
                        BenchmarkPrice bp = BenchmarkPrice.of(ticker, date, close);
                        bp.updatePrices(
                            detail.openPrice(), detail.highPrice(), detail.lowPrice(), 
                            close, changeRate, detail.volume()
                        );
                        benchmarkPricePort.save(bp);
                    }
                );
    }

    private BigDecimal calculateChangeRate(BenchmarkPriceData detail, BigDecimal close) {
        BigDecimal changeRate = detail.prdyCtrt();

        // 만약 API에서 등락률을 제공하지 않는 경우 직접 계산 (안전장치)
        if (changeRate == null || changeRate.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal prdyVrss = detail.prdyVrss() != null ? detail.prdyVrss() : BigDecimal.ZERO;
            BigDecimal prevClose = close.subtract(prdyVrss);
            return prevClose.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                    prdyVrss.divide(prevClose, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }

        return changeRate;
    }

}
