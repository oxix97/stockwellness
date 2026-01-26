package org.stockwellness.adapter.in.batch.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.persistence.stock.repository.StockHistoryRepository;
import org.stockwellness.domain.stock.StockCandle;
import org.stockwellness.domain.stock.StockHistory;
import org.stockwellness.domain.stock.TechnicalIndicators;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockHistoryProcessor implements ItemProcessor<String, List<StockHistory>> {

    private final StockHistoryRepository stockHistoryRepository;
    private final TechnicalIndicatorService technicalIndicatorService;

    @Override
    public List<StockHistory> process(String isinCode) throws Exception {
        // 1. 해당 종목의 최근 히스토리 조회 (최대 300일)
        List<StockHistory> rawHistory = stockHistoryRepository.findRecentHistory(isinCode, LocalDate.now(), 300);

        if (rawHistory.isEmpty()) {
            return null; // 데이터가 없으면 Skip
        }

        // 2. 지표 계산을 위해 과거 -> 현재 순으로 재정렬
        List<StockHistory> historyList = new ArrayList<>(rawHistory);
        historyList.sort(Comparator.comparing(StockHistory::getBaseDate));

        // 3. 지표 계산을 위한 Candle 변환
        List<StockCandle> candles = historyList.stream()
                .map(StockHistory::toCandle)
                .toList();

        // 3. 서비스에 계산 위임
        Map<LocalDate, TechnicalIndicators> indicatorsMap = technicalIndicatorService.calculate(candles);

        // 4. 전체 기간을 순회하며 엔티티에 계산된 값 업데이트
        for (StockHistory entity : historyList) {
            TechnicalIndicators indicators = indicatorsMap.get(entity.getBaseDate());
            if (indicators != null) {
                entity.updateIndicators(indicators);
            }
        }

        log.debug("Calculated indicators for ISIN: {} (Rows: {})", isinCode, historyList.size());

        // 5. 값이 업데이트된 엔티티 리스트 반환 (Writer로 전달됨)
        return historyList;
    }
}