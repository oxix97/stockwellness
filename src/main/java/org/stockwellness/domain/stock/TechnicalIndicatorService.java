package org.stockwellness.domain.stock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TechnicalIndicatorService {

    private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");

    /**
     * 전체 Candle 리스트를 받아 지표를 일괄 계산합니다.
     * ta4j는 시계열 순서가 중요하므로, 내부적으로 날짜 오름차순 정렬을 수행합니다.
     */
    public Map<LocalDate, TechnicalIndicators> calculate(List<StockCandle> candles) {
        if (candles == null || candles.isEmpty()) {
            return new HashMap<>();
        }

        // 1. 데이터 정렬 (과거 -> 현재)
        List<StockCandle> sortedCandles = candles.stream()
                .sorted(Comparator.comparing(StockCandle::baseDate))
                .toList();

        // 2. BarSeries 생성 (ta4j 컨테이너)
        BarSeries series = new BaseBarSeries("stock_series");
        for (StockCandle candle : sortedCandles) {
            ZonedDateTime zdt = candle.baseDate().atStartOfDay(ZoneId.systemDefault());
            // ta4j Bar 생성 (시가, 고가, 저가, 종가, 거래량)
            // BigDecimal -> ta4j Num 타입 변환은 내부적으로 처리됨 (double or String)
            series.addBar(zdt,
                    candle.open(),
                    candle.high(),
                    candle.low(),
                    candle.close(),
                    candle.volume());
        }

        // 3. 지표 정의
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // MA (이동평균)
        SMAIndicator ma5Indicator = new SMAIndicator(closePrice, 5);
        SMAIndicator ma20Indicator = new SMAIndicator(closePrice, 20);

        // RSI (14일)
        RSIIndicator rsi14Indicator = new RSIIndicator(closePrice, 14);

        // MACD (12, 26) - 표준 설정
        MACDIndicator macdIndicator = new MACDIndicator(closePrice, 12, 26);

        // 4. 결과 매핑 (날짜 -> 지표)
        Map<LocalDate, TechnicalIndicators> resultMap = new HashMap<>();
        int barCount = series.getBarCount();

        for (int i = 0; i < barCount; i++) {
            LocalDate date = series.getBar(i).getEndTime().toLocalDate();

            // 각 지표 값 추출 (데이터 부족 시 0 처리)
            BigDecimal ma5 = safeGet(ma5Indicator, i);
            BigDecimal ma20 = safeGet(ma20Indicator, i);
            BigDecimal rsi14 = safeGet(rsi14Indicator, i);
            BigDecimal macd = safeGet(macdIndicator, i);

            resultMap.put(date, new TechnicalIndicators(ma5, ma20, rsi14, macd));
        }

        return resultMap;
    }

    // ta4j Indicator 값 -> BigDecimal 변환 및 Null/NaN 안전 처리
    private BigDecimal safeGet(org.ta4j.core.Indicator<?> indicator, int index) {
        try {
            Object value = indicator.getValue(index);
            if (value instanceof DecimalNum num) {
                if (num.isNaN()) return BigDecimal.ZERO;
                // DB numeric(19, 2~4) 스펙에 맞춰 반올림 (여기서는 4자리로 통일)
                return BigDecimal.valueOf(num.doubleValue())
                        .setScale(4, RoundingMode.HALF_UP);
            }
            // Number 타입인 경우
            if (value instanceof Number num) {
                return BigDecimal.valueOf(num.doubleValue())
                        .setScale(4, RoundingMode.HALF_UP);
            }
        } catch (Exception e) {
            // 인덱스 범위 초과 등 예외 발생 시 0 반환
            return BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    /**
     * [Batch Backfill용]
     * 특정 종목의 전체 히스토리 리스트를 받아, 모든 날짜의 기술적 지표를 일괄 재계산합니다.
     * 기존 convertToBarSeries와 toBigDecimal 메서드를 재사용합니다.
     *
     * @param histories 해당 종목의 전체 히스토리 (날짜 무관)
     * @return 지표가 계산되어 업데이트된 리스트
     */
    public List<StockHistory> calculateFullHistory(List<StockHistory> histories) {
        // 1. 방어 로직
        if (histories == null || histories.isEmpty()) {
            return histories;
        }

        // 2. 정렬 (Time Series 보장): 과거 -> 현재 순서로 정렬
        // BarSeries의 인덱스와 List의 인덱스를 일치시키기 위해 필수입니다.
        histories.sort(Comparator.comparing(StockHistory::getBaseDate));

        // 3. Ta4j BarSeries 변환 (기존 메서드 재사용)
        // 리스트의 첫 번째 요소에서 종목 코드를 가져옵니다.
        BarSeries series = convertToBarSeries(histories.get(0).getIsinCode(), histories);

        // 4. 지표 계산 엔진 준비 (루프 밖에서 1회 생성)
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma5 = new SMAIndicator(closePrice, 5);
        SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);

        // 5. 전체 루프를 돌며 값 주입
        // series.getBarCount()는 histories.size()와 동일함이 보장됨
        for (int i = 0; i < series.getBarCount(); i++) {
            StockHistory h = histories.get(i);

            // 5-1. MA5 (데이터 5개 이상일 때부터, index 4부터)
            if (i >= 4) {
                h.updateMa5(toBigDecimal(sma5.getValue(i)));
            }

            // 5-2. MA20 (데이터 20개 이상일 때부터, index 19부터)
            if (i >= 19) {
                h.updateMa20(toBigDecimal(sma20.getValue(i)));
            }

            // 5-3. RSI (14일)
            if (i >= 13) {
                h.updateRsi14(toBigDecimal(rsi.getValue(i)));
            }

            // 5-4. MACD (12, 26) -> 26일 데이터 필요
            if (i >= 25) {
                h.updateMacd(toBigDecimal(macd.getValue(i)));
            }
        }

        return histories;
    }

    /**
     * [Realtime Daily용]
     * 과거 데이터와 현재 데이터를 합쳐 기술적 지표를 계산하고, target 엔티티에 값을 주입합니다.
     */
    public StockHistory calculateAndFill(StockHistory target, List<StockHistory> pastHistories) {
        // 1. 데이터 정합성 검증 및 정렬
        List<StockHistory> fullHistory = prepareHistoryList(target, pastHistories);

        // 2. Ta4j BarSeries 변환
        BarSeries series = convertToBarSeries(target.getIsinCode(), fullHistory);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // 지표 계산 시점: Series의 가장 마지막(오늘)
        int endIndex = series.getEndIndex();

        // 3. 지표 계산 및 주입
        if (series.getBarCount() >= 5) {
            SMAIndicator sma5 = new SMAIndicator(closePrice, 5);
            target.updateMa5(toBigDecimal(sma5.getValue(endIndex)));
        }

        if (series.getBarCount() >= 20) {
            SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
            target.updateMa20(toBigDecimal(sma20.getValue(endIndex)));
        }

        if (series.getBarCount() >= 14) {
            RSIIndicator rsi = new RSIIndicator(closePrice, 14);
            target.updateRsi14(toBigDecimal(rsi.getValue(endIndex)));
        }

        if (series.getBarCount() >= 26) {
            MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
            target.updateMacd(toBigDecimal(macd.getValue(endIndex)));
        }

        return target;
    }

    /**
     * 과거 데이터와 현재 데이터를 합치고 날짜순으로 정렬
     */
    private List<StockHistory> prepareHistoryList(StockHistory target, List<StockHistory> past) {
        if (past == null) {
            return List.of(target);
        }
        List<StockHistory> sortedList = new java.util.ArrayList<>(past);
        sortedList.add(target);
        sortedList.sort(Comparator.comparing(StockHistory::getBaseDate));
        return sortedList;
    }

    /**
     * Entity List -> Ta4j BarSeries 변환
     */
    private BarSeries convertToBarSeries(String ticker, List<StockHistory> histories) {
        BarSeries series = new BaseBarSeriesBuilder().withName(ticker).build();

        for (StockHistory h : histories) {
            ZonedDateTime zdt = h.getBaseDate().atStartOfDay(ZONE_KST);

            BigDecimal open = h.getOpenPrice() != null ? h.getOpenPrice() : BigDecimal.ZERO;
            BigDecimal high = h.getHighPrice() != null ? h.getHighPrice() : open;
            BigDecimal low = h.getLowPrice() != null ? h.getLowPrice() : open;
            BigDecimal close = h.getClosePrice() != null ? h.getClosePrice() : open;
            BigDecimal volume = h.getVolume() != null ? BigDecimal.valueOf(h.getVolume()) : BigDecimal.ZERO;

            series.addBar(zdt, open, high, low, close, volume);
        }
        return series;
    }

    /**
     * Ta4j Num -> Java BigDecimal 변환 (Scale 조정)
     */
    private BigDecimal toBigDecimal(Num num) {
        if (num == null || num.isNaN()) return null;
        return BigDecimal.valueOf(num.doubleValue())
                .setScale(4, RoundingMode.HALF_UP);
    }
}