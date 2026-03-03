package org.stockwellness.domain.stock.analysis;

import lombok.extern.slf4j.Slf4j;
import org.stockwellness.domain.stock.price.AlignmentStatus;
import org.stockwellness.domain.stock.price.TechnicalIndicators;
import org.stockwellness.global.util.QuantMapper;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.adx.MinusDIIndicator;
import org.ta4j.core.indicators.adx.PlusDIIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.stockwellness.global.util.QuantMapper.toBigDecimal;

/**
 * ta4j 라이브러리를 이용한 기술적 지표 계산기
 */
@Slf4j
public class TechnicalIndicatorCalculator {

    /**
     * 가상 날짜를 사용하여 종가 시리즈에 대한 지표 리스트를 계산합니다.
     */
    public static List<TechnicalIndicators> calculateSeries(List<BigDecimal> closingPrices) {
        if (closingPrices == null || closingPrices.isEmpty()) {
            return Collections.emptyList();
        }
        BarSeries series = QuantMapper.toBarSeries("QuantSeries", closingPrices, closingPrices, closingPrices);
        return calculateFromSeries(series, closingPrices.size());
    }

    /**
     * 실제 날짜와 종가 시리즈를 받아 지표 리스트를 계산합니다.
     */
    public static List<TechnicalIndicators> calculateSeries(List<BigDecimal> closingPrices, List<LocalDate> dates) {
        return calculateSeries(closingPrices, closingPrices, closingPrices, dates);
    }

    /**
     * OHLC 데이터와 날짜 정보를 받아 정확한 지표 시리즈를 계산합니다.
     */
    public static List<TechnicalIndicators> calculateSeries(
            List<BigDecimal> highPrices,
            List<BigDecimal> lowPrices,
            List<BigDecimal> closePrices,
            List<LocalDate> dates
    ) {
        if (closePrices == null || closePrices.isEmpty()) {
            return Collections.emptyList();
        }
        BarSeries series = QuantMapper.toBarSeries("QuantSeries", highPrices, lowPrices, closePrices, dates);
        return calculateFromSeries(series, closePrices.size());
    }

    /**
     * 가장 최근의 기술적 지표 하나만 계산합니다.
     */
    public static TechnicalIndicators calculateLatest(List<BigDecimal> closingPrices) {
        List<TechnicalIndicators> series = calculateSeries(closingPrices);
        return series.isEmpty() ? TechnicalIndicators.empty() : series.get(series.size() - 1);
    }

    private static List<TechnicalIndicators> calculateFromSeries(BarSeries series, int originalSize) {
        if (series.getBarCount() == 0) {
            return new ArrayList<>(Collections.nCopies(originalSize, TechnicalIndicators.empty()));
        }
        ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);

        // 1. 이동평균선 (SMA)
        SMAIndicator ma5 = new SMAIndicator(closePriceIndicator, 5);
        SMAIndicator ma20 = new SMAIndicator(closePriceIndicator, 20);
        SMAIndicator ma60 = new SMAIndicator(closePriceIndicator, 60);
        SMAIndicator ma120 = new SMAIndicator(closePriceIndicator, 120);

        // 2. RSI & MACD
        RSIIndicator rsi14 = new RSIIndicator(closePriceIndicator, 14);
        MACDIndicator macd = new MACDIndicator(closePriceIndicator, 12, 26);
        SMAIndicator macdSignal = new SMAIndicator(macd, 9);

        // 3. Bollinger Bands (20, 2)
        StandardDeviationIndicator sd20 = new StandardDeviationIndicator(closePriceIndicator, 20);
        BollingerBandsMiddleIndicator bbMid = new BollingerBandsMiddleIndicator(ma20);
        BollingerBandsUpperIndicator bbUpper = new BollingerBandsUpperIndicator(bbMid, sd20, series.numOf(2));
        BollingerBandsLowerIndicator bbLower = new BollingerBandsLowerIndicator(bbMid, sd20, series.numOf(2));

        // 4. ADX (14)
        PlusDIIndicator plusDI = new PlusDIIndicator(series, 14);
        MinusDIIndicator minusDI = new MinusDIIndicator(series, 14);
        ADXIndicator adx = new ADXIndicator(series, 14);

        List<TechnicalIndicators> results = new ArrayList<>();
        for (int i = 0; i < series.getBarCount(); i++) {
            BigDecimal m5 = getIndicatorValue(ma5, i, 5);
            BigDecimal m20 = getIndicatorValue(ma20, i, 20);
            BigDecimal m60 = getIndicatorValue(ma60, i, 60);
            BigDecimal m120 = getIndicatorValue(ma120, i, 120);

            // [수정] 데이터가 부족하더라도 null이 아닌 MIXED를 기본으로 반환하도록 함
            AlignmentStatus alignment = resolveAlignment(m5, m20, m60, m120);
            
            Boolean isGolden = (i > 0) ? isGoldenCross(ma5, ma20, i) : null;
            Boolean isDead = (i > 0) ? isDeadCross(ma5, ma20, i) : null;

            results.add(new TechnicalIndicators(
                    m5, m20, m60, m120,
                    getIndicatorValue(rsi14, i, 14),
                    getIndicatorValue(macd, i, 26),
                    getIndicatorValue(macdSignal, i, 35),
                    toBigDecimal(bbUpper.getValue(i)),
                    toBigDecimal(bbMid.getValue(i)),
                    toBigDecimal(bbLower.getValue(i)),
                    toBigDecimal(adx.getValue(i)),
                    toBigDecimal(plusDI.getValue(i)),
                    toBigDecimal(minusDI.getValue(i)),
                    alignment, // 이 값이 null이 되지 않도록 보장
                    isGolden,
                    isDead,
                    isMacdCross(macd, macdSignal, i)
            ));
        }

        // 입력 사이즈와 맞추기 위해 앞에 null 패딩 (데이터가 부족하거나 null인 경우)
        if (results.size() < originalSize) {
            List<TechnicalIndicators> paddedResults = new ArrayList<>(Collections.nCopies(originalSize - results.size(), TechnicalIndicators.empty()));
            paddedResults.addAll(results);
            return paddedResults;
        }

        return results;
    }

    private static BigDecimal getIndicatorValue(Indicator<Num> indicator, int index, int requiredBarCount) {
        if (index < requiredBarCount - 1) {
            return null;
        }
        return toBigDecimal(indicator.getValue(index));
    }

    private static AlignmentStatus resolveAlignment(BigDecimal m5, BigDecimal m20, BigDecimal m60, BigDecimal m120) {
        if (m5 == null || m20 == null || m60 == null || m120 == null) return AlignmentStatus.MIXED;
        if (m5.compareTo(m20) > 0 && m20.compareTo(m60) > 0 && m60.compareTo(m120) > 0) return AlignmentStatus.PERFECT;
        if (m5.compareTo(m20) < 0 && m20.compareTo(m60) < 0 && m60.compareTo(m120) < 0) return AlignmentStatus.REVERSE;
        return AlignmentStatus.MIXED;
    }

    private static boolean isGoldenCross(Indicator<Num> fast, Indicator<Num> slow, int index) {
        return fast.getValue(index).isGreaterThan(slow.getValue(index)) &&
               fast.getValue(index - 1).isLessThanOrEqual(slow.getValue(index - 1));
    }

    private static boolean isDeadCross(Indicator<Num> fast, Indicator<Num> slow, int index) {
        return fast.getValue(index).isLessThan(slow.getValue(index)) &&
               fast.getValue(index - 1).isGreaterThanOrEqual(slow.getValue(index - 1));
    }

    private static Boolean isMacdCross(MACDIndicator macd, SMAIndicator signal, int index) {
        if (index <= 0) return false;
        return isGoldenCross(macd, signal, index);
    }
}
