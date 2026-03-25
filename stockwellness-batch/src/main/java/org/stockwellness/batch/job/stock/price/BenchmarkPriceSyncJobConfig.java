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
import org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail;
import org.stockwellness.adapter.out.persistence.stock.repository.BenchmarkPriceRepository;
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
    private final BenchmarkPriceRepository benchmarkPriceRepository;

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
            LocalDate startDate = today.minusYears(2);

            for (BenchmarkType type : BenchmarkType.values()) {
                String ticker = type.getTicker();
                log.info("[Benchmark] 시세 수집 시작: {} ({})", type.getDescription(), ticker);
                
                List<KisDailyPriceDetail> details = kisAdapter.fetchIndexDailyPrices(ticker, startDate, today);
                
                for (KisDailyPriceDetail detail : details) {
                    saveBenchmarkPrice(ticker, detail);
                }
                log.info("[Benchmark] 시세 수집 완료: {} ({}건)", type.getDescription(), details.size());
            }
            return RepeatStatus.FINISHED;
        };
    }

    private void saveBenchmarkPrice(String ticker, KisDailyPriceDetail detail) {
        LocalDate date = detail.baseDate();
        BigDecimal close = detail.closePrice();
        
        // 등락률 계산 (전일대비 / (종가 - 전일대비) * 100)
        BigDecimal prdyVrss = detail.prdyVrss() != null ? detail.prdyVrss() : BigDecimal.ZERO;
        BigDecimal prevClose = close.subtract(prdyVrss);
        BigDecimal changeRate = prevClose.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                prdyVrss.divide(prevClose, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        benchmarkPriceRepository.findByTickerAndBaseDate(ticker, date)
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
                        benchmarkPriceRepository.save(bp);
                    }
                );
    }
}
