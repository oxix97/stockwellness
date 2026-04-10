package org.stockwellness.batch.job.investortradedetail.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.global.util.DateUtil;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockInvestorTradeDetailSummaryListener implements StepExecutionListener {

    private final StockPriceRepository stockPriceRepository;

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String baseDateParam = stepExecution.getJobParameters().getString("baseDate");
        if (baseDateParam == null || baseDateParam.isBlank()) {
            return stepExecution.getExitStatus();
        }

        LocalDate baseDate = DateUtil.parse(baseDateParam);
        long totalCount = stockPriceRepository.countByBaseDate(baseDate);
        long nonZeroSupplyCount = stockPriceRepository.countByBaseDateAndNonZeroSupply(baseDate);

        log.info(
                "[투자주체 수급 보정 Summary] baseDate={}, readCount={}, writeCount={}, totalStockPriceCount={}, nonZeroSupplyCount={}",
                baseDate,
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                totalCount,
                nonZeroSupplyCount
        );

        if (nonZeroSupplyCount == 0) {
            log.warn("[투자주체 수급 보정 Summary] 비영 수급 데이터가 없습니다. baseDate={}", baseDate);
        }

        return stepExecution.getExitStatus();
    }
}
