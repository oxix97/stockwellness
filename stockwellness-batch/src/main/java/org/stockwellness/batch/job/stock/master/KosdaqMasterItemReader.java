package org.stockwellness.batch.job.stock.master;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.support.ListItemReader;
import org.stockwellness.domain.stock.KosdaqItem;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 코스닥 마스터 ItemReader.
 *
 * <p>Step 시작 시 파싱된 단축코드(ticker) Set을 Job {@link ExecutionContext}에 저장합니다.
 * {@link StockDelistTasklet}이 이 값을 꺼내 상장폐지 대상을 판별합니다.
 */
@Slf4j
public class KosdaqMasterItemReader extends ListItemReader<KosdaqItem>
        implements StepExecutionListener {

    public static final String CTX_KEY_ACTIVE_CODES = "kosdaq.activeTickers";

    private final List<KosdaqItem> items;

    public KosdaqMasterItemReader(List<KosdaqItem> items) {
        super(items);
        this.items = items;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        Set<String> activeTickers = items.stream()
                .map(KosdaqItem::shortCode)
                .collect(Collectors.toSet());

        stepExecution.getJobExecution().getExecutionContext()
                .put(CTX_KEY_ACTIVE_CODES, activeTickers);

        log.info("[KOSDAQ] Reader: 활성 종목 {}건을 ExecutionContext에 저장", activeTickers.size());
    }
}
