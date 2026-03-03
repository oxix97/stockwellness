package org.stockwellness.batch.job.stock.master;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.domain.stock.MarketType;

import java.util.Set;

/**
 * 상장폐지 종목 일괄 처리 Tasklet.
 *
 * <p>이전 Upsert Step의 Reader가 Job {@code ExecutionContext}에 저장한
 * 활성 ticker Set을 꺼내, DB에서 해당 시장의 비폐지 종목 중
 * Set에 없는 종목을 {@code DELISTED}로 일괄 UPDATE합니다.
 */
@Slf4j
@RequiredArgsConstructor
public class StockDelistTasklet implements Tasklet {

    private final StockRepository stockRepository;
    private final MarketType marketType;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        Set<String> activeTickers = getActiveTickers(chunkContext);

        if (activeTickers == null || activeTickers.isEmpty()) {
            log.warn("[{}] 활성 ticker Set이 비어있어 상장폐지 처리를 건너뜁니다. (데이터 유실 방지)", marketType);
            contribution.setExitStatus(new ExitStatus("SKIPPED", "activeTickers empty"));
            return RepeatStatus.FINISHED;
        }

        log.info("[{}] 상장폐지 처리 시작: 마스터 파일 활성 종목 {}건", marketType, activeTickers.size());

        int count = stockRepository.delistMissingStocks(marketType, activeTickers);
        contribution.incrementWriteCount(count);

        log.info("[{}] 상장폐지 처리 완료: {}건", marketType, count);
        return RepeatStatus.FINISHED;
    }

    @SuppressWarnings("unchecked")
    private Set<String> getActiveTickers(ChunkContext chunkContext) {
        var jobCtx = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext();

        String ctxKey = switch (marketType) {
            case KOSPI  -> KospiMasterItemReader.CTX_KEY_ACTIVE_CODES;
            case KOSDAQ -> KosdaqMasterItemReader.CTX_KEY_ACTIVE_CODES;
            default     -> throw new IllegalStateException("지원하지 않는 marketType: " + marketType);
        };

        return (Set<String>) jobCtx.get(ctxKey);
    }
}
