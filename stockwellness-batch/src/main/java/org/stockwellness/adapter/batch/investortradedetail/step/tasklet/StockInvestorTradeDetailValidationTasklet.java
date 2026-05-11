package org.stockwellness.adapter.batch.investortradedetail.step.tasklet;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.stockwellness.application.service.batch.StockInvestorTradeDetailBatchService;

@RequiredArgsConstructor
public class StockInvestorTradeDetailValidationTasklet implements Tasklet {

    private final StockInvestorTradeDetailBatchService batchService;
    private final LocalDate requestedDate;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDate resolvedBaseDate = batchService.resolveMarketBaseDate(requestedDate);
        var executionContext = contribution.getStepExecution().getJobExecution().getExecutionContext();
        executionContext.putString("requestedDate", requestedDate.toString());
        executionContext.putString("resolvedBaseDate", resolvedBaseDate.toString());
        executionContext.put("stockPriceExists", true);
        return RepeatStatus.FINISHED;
    }
}
