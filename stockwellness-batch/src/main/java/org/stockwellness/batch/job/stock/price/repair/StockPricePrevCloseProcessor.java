package org.stockwellness.batch.job.stock.price.repair;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.stockwellness.adapter.out.persistence.stock.repository.StockPriceRepository;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;

import org.stockwellness.global.util.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class StockPricePrevCloseProcessor implements ItemProcessor<Stock, List<StockPriceRepairDto>> {

    private final StockPriceRepository stockPriceRepository;

    @Value("#{jobParameters['startDate']}")
    private String startDateStr;

    @Value("#{jobParameters['endDate']}")
    private String endDateStr;

    @Override
    public List<StockPriceRepairDto> process(Stock stock) {
        // [중요] 전체 시세를 가져와서 논리적인 연결 고리를 처음부터 추적함
        List<StockPrice> allPrices = stockPriceRepository.findByStockIdOrderByBaseDateAsc(stock.getId());
        if (allPrices.isEmpty()) return null;

        LocalDate reqStart = DateUtil.parse(startDateStr);
        LocalDate reqEnd = DateUtil.parse(endDateStr);

        List<StockPriceRepairDto> toUpdate = new ArrayList<>();
        BigDecimal previousClose = null;

        for (StockPrice current : allPrices) {
            LocalDate currentBaseDate = current.getId().getBaseDate();
            
            // 1. 전날 데이터가 존재하는 경우에만 보정 가능
            if (previousClose != null) {
                // 2. 요청받은 날짜 범위 내에 있는지 확인
                boolean inRange = DateUtil.isBetween(currentBaseDate, reqStart, reqEnd);

                // 3. 보정이 필요한 상태인지 확인 (NULL 또는 0)
                boolean needsRepair = current.getPreviousClosePrice() == null 
                                   || current.getPreviousClosePrice().compareTo(BigDecimal.ZERO) == 0;

                if (inRange && needsRepair) {
                    toUpdate.add(new StockPriceRepairDto(
                            stock.getId(),
                            currentBaseDate,
                            previousClose
                    ));
                }
            }
            
            // 다음 루프를 위해 오늘의 종가를 '전일 종가' 후보로 저장
            previousClose = current.getClosePrice();
        }

        if (!toUpdate.isEmpty()) {
            log.info(">>> Stock {}: Identified {} rows to repair", stock.getTicker(), toUpdate.size());
        }
        return toUpdate.isEmpty() ? null : toUpdate;
    }
}
