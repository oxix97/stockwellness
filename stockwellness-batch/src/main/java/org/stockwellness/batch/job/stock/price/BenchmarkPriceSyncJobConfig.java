package org.stockwellness.batch.job.stock.price;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.BenchmarkPriceData;
import org.stockwellness.application.port.out.stock.BenchmarkPricePort;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.domain.stock.price.BenchmarkPrice;
import org.stockwellness.batch.exception.BatchException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
                .<BenchmarkType, List<BenchmarkPrice>>chunk(1, transactionManager) // 각 지수 타입별로 처리
                .reader(benchmarkTypeReader())
                .processor(benchmarkPriceProcessor(null))
                .writer(benchmarkPriceWriter())
                .build();
    }

    @Bean
    public ListItemReader<BenchmarkType> benchmarkTypeReader() {
        // 모든 벤치마크 지수 타입을 읽어옴
        return new ListItemReader<>(Arrays.asList(BenchmarkType.values()));
    }

    @Bean
    @StepScope
    public ItemProcessor<BenchmarkType, List<BenchmarkPrice>> benchmarkPriceProcessor(
            @Value("#{jobParameters['startDate']}") String startDateParam) {
        return type -> {
            LocalDate today = LocalDate.now();
            LocalDate startDate = startDateParam != null ? LocalDate.parse(startDateParam) : today.minusYears(2);
            LocalDate endDate = today; // [버그 수정] 미정의되었던 endDate 설정

            log.info("[지수 동기화] 시세 수집 시작: {} (API: {}, 기간: {} ~ {})",
                    type.getDescription(), type.getApiTicker(), startDate, endDate);

            try {
                List<BenchmarkPriceData> details;
                if (type.isOverseas()) {
                    details = kisAdapter.fetchOverseasIndexDailyPrices(type.getApiTicker(), startDate, endDate);
                } else {
                    details = kisAdapter.fetchIndexDailyPrices(type.getApiTicker(), startDate);
                }

                if (details == null || details.isEmpty()) {
                    log.warn("[지수 동기화] 수집된 데이터가 없습니다: {}", type.getDescription());
                    return List.of();
                }

                List<BenchmarkPrice> prices = details.stream()
                        .map(detail -> createBenchmarkPrice(type.getTicker(), detail))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());

                log.info("[지수 동기화] {} 지수 데이터 변환 완료: {}건", type.getDescription(), prices.size());
                return prices;

            } catch (BatchException e) {
                log.error("[지수 동기화] 비즈니스 오류 발생: {} - {}", type.getDescription(), e.getMessage());
                return List.of(); 
            } catch (Exception e) {
                log.error("[지수 동기화] 시세 수집 중 예기치 않은 시스템 오류 발생: {} - {}", type.getDescription(), e.getMessage());
                return List.of();
            }
        };
    }

    @Bean
    public ItemWriter<List<BenchmarkPrice>> benchmarkPriceWriter() {
        return lists -> {
            int totalSaved = 0;
            for (List<BenchmarkPrice> prices : lists) {
                for (BenchmarkPrice price : prices) {
                    saveOrUpdateBenchmarkPrice(price);
                    totalSaved++;
                }
            }
            log.info("[지수 동기화] 총 {}건의 데이터 저장/업데이트 완료", totalSaved);
        };
    }

    private Optional<BenchmarkPrice> createBenchmarkPrice(String ticker, BenchmarkPriceData detail) {
        LocalDate date = detail.baseDate();
        BigDecimal close = detail.closePrice();

        if (date == null || close == null || close.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        BigDecimal changeRate = calculateChangeRate(detail, close);
        BenchmarkPrice bp = BenchmarkPrice.of(ticker, date, close);
        bp.updatePrices(
                detail.openPrice(), detail.highPrice(), detail.lowPrice(),
                close, changeRate, detail.volume()
        );
        return Optional.of(bp);
    }

    private void saveOrUpdateBenchmarkPrice(BenchmarkPrice bp) {
        benchmarkPricePort.findByTickerAndBaseDate(bp.getTicker(), bp.getBaseDate())
                .ifPresentOrElse(
                        existing -> existing.updatePrices(
                                bp.getOpenPrice(), bp.getHighPrice(), bp.getLowPrice(),
                                bp.getClosePrice(), bp.getChangeRate(), bp.getVolume()
                        ),
                        () -> benchmarkPricePort.save(bp)
                );
    }

    private BigDecimal calculateChangeRate(BenchmarkPriceData detail, BigDecimal close) {
        BigDecimal changeRate = detail.prdyCtrt();

        // API에서 등락률을 제공하지 않는 경우(예: 해외지수 일부) 직접 계산
        if (changeRate == null || changeRate.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal prdyVrss = detail.prdyVrss() != null ? detail.prdyVrss() : BigDecimal.ZERO;
            BigDecimal prevClose = close.subtract(prdyVrss);
            return prevClose.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                    prdyVrss.divide(prevClose, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }

        return changeRate;
    }
}
