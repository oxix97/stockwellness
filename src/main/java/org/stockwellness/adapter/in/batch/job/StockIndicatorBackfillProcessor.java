package org.stockwellness.adapter.in.batch.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.StockHistoryRepository;
import org.stockwellness.application.service.mapper.StockMapper;
import org.stockwellness.domain.stock.StockCandle;
import org.stockwellness.domain.stock.StockHistory;
import org.stockwellness.domain.stock.TechnicalIndicatorService;
import org.stockwellness.domain.stock.TechnicalIndicators;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StockIndicatorBackfillProcessor implements ItemProcessor<String, List<StockHistory>> {

    private final StockHistoryRepository stockHistoryRepository;
    private final TechnicalIndicatorService calculator; // 이전에 만든 도메인 서비스
    private final StockMapper stockMapper;

    @Override
    public List<StockHistory> process(String isinCode) {
        // 1. DB 조회
        List<StockHistory> histories = stockHistoryRepository.findAllByIsinCodeOrderByBaseDateAsc(isinCode);

        // 2. Entity -> Candle 변환 (Mapper 사용!)
        List<StockCandle> candles = histories.stream()
                .map(stockMapper::toStockCandle) // 여기서 사용
                .toList();

        // 3. 지표 일괄 계산 (ta4j 활용)
        Map<LocalDate, TechnicalIndicators> indicatorsMap = calculator.calculate(candles);

        // 4. Entity에 계산된 지표 업데이트 (Dirty Checking이 아닌 명시적 Update를 위해 객체 값 변경)
        histories.forEach(h -> {
            TechnicalIndicators indicators = indicatorsMap.get(h.getBaseDate());
            if (indicators != null) {
                h.updateIndicators(indicators); // Entity 내부에 비즈니스 메서드 구현 필요
            }
        });

        return histories; // 업데이트된 엔티티 리스트 반환
    }

    private StockCandle toCandle(StockHistory h) {
        return new StockCandle(
            h.getBaseDate(),
            h.getOpenPrice(), h.getHighPrice(), h.getLowPrice(), h.getClosePrice(), h.getVolume()
        );
    }
}