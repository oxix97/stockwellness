package org.stockwellness.adapter.in.batch.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.StockHistoryRepository;
import org.stockwellness.domain.stock.StockHistory;
import org.stockwellness.domain.stock.TechnicalIndicators;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockHistoryProcessor implements ItemProcessor<String, List<StockHistory>> {

    private final StockHistoryRepository stockHistoryRepository;

    @Override
    public List<StockHistory> process(String isinCode) throws Exception {
        // 1. 해당 종목의 전체 히스토리 조회 (날짜 오름차순 필수)
        List<StockHistory> historyList = stockHistoryRepository.findAllByIsinCodeOrderByBaseDateAsc(isinCode);

        if (historyList.isEmpty()) {
            return null; // 데이터가 없으면 Skip
        }

        // 2. ta4j 계산을 위한 BarSeries 변환
        BarSeries series = createBarSeries(isinCode, historyList);

        // 3. 지표 계산기 초기화 (계산 규칙 정의)
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 이동평균선 (SMA)
        SMAIndicator ma5 = new SMAIndicator(closePrice, 5);
        SMAIndicator ma20 = new SMAIndicator(closePrice, 20);
        SMAIndicator ma60 = new SMAIndicator(closePrice, 60);
        SMAIndicator ma120 = new SMAIndicator(closePrice, 120);

        // RSI (14)
        RSIIndicator rsi14 = new RSIIndicator(closePrice, 14);

        // MACD (12, 26) - Signal Line이 아닌 MACD Line 값
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);

        // 4. 전체 기간을 순회하며 엔티티에 계산된 값 업데이트
        for (int i = 0; i < series.getBarCount(); i++) {
            StockHistory entity = historyList.get(i);
            TechnicalIndicators indicators = TechnicalIndicators.of(
                    numToBigDecimal(ma5.getValue(i)),
                    numToBigDecimal(ma20.getValue(i)),
                    numToBigDecimal(ma60.getValue(i)),
                    numToBigDecimal(ma120.getValue(i)),
                    numToBigDecimal(rsi14.getValue(i)),
                    numToBigDecimal(macd.getValue(i)));

            entity.updateIndicators(indicators);
        }

        log.debug("Calculated indicators for ISIN: {} (Rows: {})", isinCode, historyList.size());

        // 5. 값이 업데이트된 엔티티 리스트 반환 (Writer로 전달됨)
        return historyList;
    }

    /**
     * StockHistory 리스트를 ta4j BarSeries로 변환
     */
    private BarSeries createBarSeries(String isinCode, List<StockHistory> historyList) {
        BarSeries series = new BaseBarSeries(isinCode);

        for (StockHistory h : historyList) {
            series.addBar(
                    h.getBaseDate().atStartOfDay(ZoneId.systemDefault()), // LocalDate -> ZonedDateTime
                    h.getOpenPrice(),
                    h.getHighPrice(),
                    h.getLowPrice(),
                    h.getClosePrice(),
                    h.getVolume()
            );
        }
        return series;
    }

    /**
     * ta4j Num 타입을 BigDecimal로 변환 (NaN 처리 포함)
     */
    private BigDecimal numToBigDecimal(Num num) {
        if (num.isNaN()) {
            return null; // DB에 null로 저장
        }
        // ta4j 계산 결과를 BigDecimal로 변환 (소수점 처리가 필요하다면 setScale 추가)
        return BigDecimal.valueOf(num.doubleValue());
    }
}