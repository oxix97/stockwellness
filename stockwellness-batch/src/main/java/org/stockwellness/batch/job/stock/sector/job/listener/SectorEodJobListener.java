package org.stockwellness.batch.job.stock.sector.job.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.stock.SectorInsightUseCase;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.insight.event.SectorInsightUpdatedEvent;

import java.time.LocalDate;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class SectorEodJobListener implements JobExecutionListener {

    private final ApplicationEventPublisher eventPublisher;
    private final CacheManager cacheManager;
    private final SectorInsightUseCase sectorInsightUseCase;

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("SectorEodJob 성공적으로 완료. 캐시를 무효화하고 프리워밍을 시작합니다.");
            
            // 1. 캐시 무효화
            evictSectorCaches();
            
            // 2. 캐시 프리워밍 (가장 많이 쓰이는 상위 10개 기준)
            LocalDate today = LocalDate.now();
            try {
                sectorInsightUseCase.getTopSectorsByFluctuation(today, 10);
                sectorInsightUseCase.getTopSectorsBySupply(today, 10);
                log.info("섹터 캐시 프리워밍 완료.");
            } catch (Exception e) {
                log.error("캐시 프리워밍 중 오류 발생: {}", e.getMessage());
            }
            
            eventPublisher.publishEvent(new SectorInsightUpdatedEvent(
                MarketType.KOSPI, 
                today, 
                "COMPLETED"
            ));
        }
    }

    private void evictSectorCaches() {
        Objects.requireNonNull(cacheManager.getCache("sectorRanking")).clear();
        Objects.requireNonNull(cacheManager.getCache("sectorSupply")).clear();
        Objects.requireNonNull(cacheManager.getCache("sectorDetail")).clear();
        log.info("섹터 관련 Redis 캐시 무효화 완료.");
    }
}
