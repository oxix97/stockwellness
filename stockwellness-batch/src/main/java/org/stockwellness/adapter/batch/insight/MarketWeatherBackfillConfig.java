package org.stockwellness.adapter.batch.insight;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.stockwellness.adapter.out.persistence.stock.repository.IndexSeriesRepository;
import org.stockwellness.application.port.in.batch.MarketDerivedDataUseCase;
import org.stockwellness.application.port.out.stock.IndexDailyReadPort;
import org.stockwellness.domain.stock.insight.IndexSeries;
import org.stockwellness.domain.stock.insight.MarketIndexKind;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MarketWeatherBackfillConfig {
    private static final int LOOKBACK = 252;
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final Set<String> KOREAN_INDEX_NAMESPACES = Set.of(
            "kis.idxcode.KOSPI", "kis.idxcode.KOSDAQ");

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final IndexSeriesRepository indexSeriesRepository;
    private final IndexDailyReadPort indexDailyReadPort;
    private final MarketDerivedDataUseCase marketDerivedDataUseCase;

    @Bean
    public Job backfillMarketWeatherJob(Step backfillMarketWeatherStep) {
        return new JobBuilder("backfillMarketWeatherJob", jobRepository)
                .start(backfillMarketWeatherStep)
                .build();
    }

    @Bean
    public Step backfillMarketWeatherStep() {
        return new StepBuilder("backfillMarketWeatherStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    backfillWeather(LocalDate.now(KOREA_ZONE));
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    void backfillWeather(LocalDate asOf) {
        List<IndexSeries> series = indexSeriesRepository.findAll().stream()
                .filter(MarketWeatherBackfillConfig::isSupportedKoreanSeries)
                .toList();
        if (series.isEmpty()) {
            throw new IllegalStateException("No canonical Korean index series are available for weather backfill");
        }

        for (IndexSeries index : series) {
            List<LocalDate> dates = indexDailyReadPort.recentFinal(index.getId(), asOf, LOOKBACK).stream()
                    .map(observation -> observation.tradeDate())
                    .distinct()
                    .sorted()
                    .toList();
            dates.forEach(date -> marketDerivedDataUseCase.calculateIndexWeather(index.getId(), date));
            log.info("Canonical market weather backfill completed for series={} dates={}", index.getId(), dates.size());
        }
    }

    private static boolean isSupportedKoreanSeries(IndexSeries series) {
        return series.isActive()
                && series.isCanonical()
                && "KIS".equals(series.getProvider())
                && KOREAN_INDEX_NAMESPACES.contains(series.getProviderNamespace())
                && Set.of("KOSPI", "KOSDAQ").contains(series.getCalendarMarketCode())
                && "KRW".equals(series.getCurrency())
                && "PRICE".equals(series.getReturnBasis())
                && "NONE".equals(series.getDividendBasis())
                && (series.getMarketIndex().getKind() == MarketIndexKind.MARKET
                    || series.getMarketIndex().getKind() == MarketIndexKind.SECTOR);
    }
}
