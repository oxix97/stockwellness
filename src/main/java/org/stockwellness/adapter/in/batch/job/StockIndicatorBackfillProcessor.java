package org.stockwellness.adapter.in.batch.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stockwellness.application.service.mapper.StockMapper;
import org.stockwellness.domain.stock.StockCandle;
import org.stockwellness.domain.stock.StockHistory;
import org.stockwellness.domain.stock.TechnicalIndicatorService;
import org.stockwellness.domain.stock.TechnicalIndicators;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@StepScope
@Component
@RequiredArgsConstructor
public class StockIndicatorBackfillProcessor implements ItemProcessor<List<StockHistory>, List<StockHistory>> {

    private final TechnicalIndicatorService calculator;
    private final StockMapper stockMapper;

    // JobParameter로 받은 날짜를 주입받아 필터링 기준으로 사용
    @Value("#{jobParameters['requestDate']}")
    private String requestDate;

    @Override
    public List<StockHistory> process(List<StockHistory> rawHistories) {
        if (rawHistories == null || rawHistories.isEmpty()) {
            return null;
        }

        // 1. 참조 데이터 준비 (지표 계산을 위해 전체 필요)
        List<StockHistory> histories = new ArrayList<>(rawHistories);
        Collections.reverse(histories); // 과거 -> 현재 순 정렬

        // 2. 변환 및 전체 계산
        List<StockCandle> candles = histories.stream()
                .map(stockMapper::toStockCandle)
                .toList();

        // indicatorsMap에는 150일치 지표가 모두 들어있음
        Map<LocalDate, TechnicalIndicators> indicatorsMap = calculator.calculate(candles);

        // 3. [핵심 수정] 타겟 날짜 파싱
        LocalDate targetDate = LocalDate.parse(requestDate, DateTimeFormatter.BASIC_ISO_DATE);

        // 4. 타겟 날짜 데이터만 필터링하여 업데이트 및 리턴
        return histories.stream()
                .filter(h -> h.getBaseDate().isEqual(targetDate)) // 요청한 날짜만 남김
                .peek(h -> {
                    TechnicalIndicators ind = indicatorsMap.get(h.getBaseDate());
                    // 앞서 만든 Null Safe 업데이트 메서드 사용
                    if (ind != null) h.updateIndicators(ind);
                })
                .toList();
    }
}