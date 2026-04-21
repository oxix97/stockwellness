package org.stockwellness.adapter.batch.investortradedetail.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;

@Slf4j
public class StockInvestorTradeDetailStepLoggingListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();
        log.info("[투자자 상세 배치] step 시작 requestedDate={}, resolvedBaseDate={}, stockPriceExists={}",
                executionContext.getString("requestedDate", "unknown"),
                executionContext.getString("resolvedBaseDate", "unknown"),
                executionContext.get("stockPriceExists"));
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();
        log.info("[투자자 상세 배치] step 완료 requestedDate={}, resolvedBaseDate={}, stockPriceExists={}, readCount={}, writeCount={}, status={}",
                executionContext.getString("requestedDate", "unknown"),
                executionContext.getString("resolvedBaseDate", "unknown"),
                executionContext.get("stockPriceExists"),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getStatus());
        return stepExecution.getExitStatus();
    }
}
