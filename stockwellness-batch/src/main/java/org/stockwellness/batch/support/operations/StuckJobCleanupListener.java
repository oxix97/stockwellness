package org.stockwellness.batch.support.operations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 서버 시작 시 DB에 STARTED 상태로 남아있는 배치를 FAILED로 정리하는 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StuckJobCleanupListener {

    private final BatchOperationsService batchOperationsService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            batchOperationsService.cleanupStuckJobs();
        } catch (Exception e) {
            log.error("[배치] 멈춰있는 배치 정리 중 오류 발생", e);
        }
    }
}
