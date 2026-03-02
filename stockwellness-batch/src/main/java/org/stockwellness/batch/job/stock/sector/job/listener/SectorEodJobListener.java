package org.stockwellness.batch.job.stock.sector.job.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.stock.SectorInsightUseCase;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SectorEodJobListener implements JobExecutionListener {

    private final CacheManager cacheManager;
    private final SectorInsightUseCase sectorInsightUseCase;

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("SectorEodJob 성공적으로 완료. 캐시를 무효화하고 프리워밍을 시작합니다.");
            
            // 1. 캐시 무효화 (안전하게 처리)
            evictSectorCaches();
            
            // 2. 캐시 프리워밍 (상위 10개 기준)
            LocalDate today = LocalDate.now();
            try {
                sectorInsightUseCase.getTopSectorsByFluctuation(today, null, 10);
                sectorInsightUseCase.getTopSectorsBySupply(today, null, 10);
                log.info("섹터 캐시 프리워밍 완료.");
            } catch (Exception e) {
                log.error("캐시 프리워밍 중 오류 발생: {}", e.getMessage());
            }
        }
    }

    private void evictSectorCaches() {
        evictCache("sectorRanking");
        evictCache("sectorSupply");
        evictCache("sectorDetail");
        evictCache("sectorComparison");
        log.info("섹터 관련 Redis 캐시 무효화 완료.");
    }

    private void evictCache(String cacheName) {
        Optional.ofNullable(cacheManager.getCache(cacheName)).ifPresent(Cache::clear);
    }
}
