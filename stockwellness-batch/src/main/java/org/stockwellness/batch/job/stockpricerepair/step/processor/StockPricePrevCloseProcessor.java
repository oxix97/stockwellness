package org.stockwellness.batch.job.stockpricerepair.step.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.batch.StockPriceRepairUseCase;
import org.stockwellness.batch.job.stockpricerepair.model.StockPriceRepairDto;
import org.stockwellness.domain.stock.Stock;

import java.util.List;

@Component
@StepScope
@RequiredArgsConstructor
public class StockPricePrevCloseProcessor implements ItemProcessor<Stock, List<StockPriceRepairDto>> {

    private final StockPriceRepairUseCase stockPriceRepairUseCase;

    @Value("#{jobParameters['startDate']}")
    private String startDateStr;

    @Value("#{jobParameters['endDate']}")
    private String endDateStr;

    @Override
    public List<StockPriceRepairDto> process(Stock stock) {
        List<StockPriceRepairDto> result = stockPriceRepairUseCase.repair(
                new StockPriceRepairUseCase.StockPriceRepairCommand(stock, startDateStr, endDateStr)
        ).rows().stream()
                .map(row -> new StockPriceRepairDto(row.stockId(), row.baseDate(), row.calculatedPrevClose()))
                .toList();
        return result.isEmpty() ? null : result;
    }
}
