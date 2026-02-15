package org.stockwellness.batch.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.stockwellness.domain.stock.TechnicalIndicators;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class TechnicalIndicatorCalculator {

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public List<TechnicalIndicators> calculateSeries(List<BigDecimal> closingPrices) {
        if (closingPrices == null || closingPrices.isEmpty()) {
            return Collections.emptyList();
        }

        int size = closingPrices.size();
        List<BigDecimal> ma5 = calculateSMASeries(closingPrices, 5);
        List<BigDecimal> ma20 = calculateSMASeries(closingPrices, 20);
        List<BigDecimal> ma60 = calculateSMASeries(closingPrices, 60);
        List<BigDecimal> ma120 = calculateSMASeries(closingPrices, 120);
        List<BigDecimal> rsi14 = calculateRSISeries(closingPrices, 14);
        List<MacdSeriesResult> macdResults = calculateMACDSeries(closingPrices);

        List<TechnicalIndicators> results = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            results.add(new TechnicalIndicators(
                    ma5.get(i), ma20.get(i), ma60.get(i), ma120.get(i),
                    rsi14.get(i),
                    macdResults.get(i).macdLine,
                    macdResults.get(i).signalLine
            ));
        }
        return results;
    }

    private List<BigDecimal> calculateSMASeries(List<BigDecimal> prices, int period) {
        int size = prices.size();
        List<BigDecimal> smaSeries = new ArrayList<>(Collections.nCopies(size, null));
        if (size < period) return smaSeries;

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (int i = 0; i < size; i++) {
            BigDecimal price = prices.get(i);
            if (price != null) {
                sum = sum.add(price);
                count++;
            }

            if (i >= period) {
                BigDecimal oldPrice = prices.get(i - period);
                if (oldPrice != null) {
                    sum = sum.subtract(oldPrice);
                    count--;
                }
            }

            if (count == period) {
                smaSeries.set(i, sum.divide(BigDecimal.valueOf(period), SCALE, ROUNDING));
            }
        }
        return smaSeries;
    }

    private List<BigDecimal> calculateRSISeries(List<BigDecimal> prices, int period) {
        int size = prices.size();
        List<BigDecimal> rsiSeries = new ArrayList<>(Collections.nCopies(size, null));
        if (size <= period) return rsiSeries;

        BigDecimal avgGain = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;

        // 초기 수집 (중간 null 무시하고 유효 데이터 기준)
        int validCount = 0;
        for (int i = 1; i < size && validCount < period; i++) {
            if (prices.get(i) != null && prices.get(i - 1) != null) {
                BigDecimal diff = prices.get(i).subtract(prices.get(i - 1));
                if (diff.compareTo(BigDecimal.ZERO) > 0) avgGain = avgGain.add(diff);
                else avgLoss = avgLoss.add(diff.abs());
                validCount++;
                
                if (validCount == period) {
                    avgGain = avgGain.divide(BigDecimal.valueOf(period), MathContext.DECIMAL128);
                    avgLoss = avgLoss.divide(BigDecimal.valueOf(period), MathContext.DECIMAL128);
                    rsiSeries.set(i, calculateRsiValue(avgGain, avgLoss));

                    // 이후 Wilder's Smoothing
                    BigDecimal p = BigDecimal.valueOf(period);
                    BigDecimal pMinus1 = BigDecimal.valueOf(period - 1);
                    for (int j = i + 1; j < size; j++) {
                        if (prices.get(j) != null && prices.get(j - 1) != null) {
                            BigDecimal d = prices.get(j).subtract(prices.get(j - 1));
                            BigDecimal g = d.compareTo(BigDecimal.ZERO) > 0 ? d : BigDecimal.ZERO;
                            BigDecimal l = d.compareTo(BigDecimal.ZERO) < 0 ? d.abs() : BigDecimal.ZERO;
                            avgGain = avgGain.multiply(pMinus1).add(g).divide(p, MathContext.DECIMAL128);
                            avgLoss = avgLoss.multiply(pMinus1).add(l).divide(p, MathContext.DECIMAL128);
                            rsiSeries.set(j, calculateRsiValue(avgGain, avgLoss));
                        } else {
                            rsiSeries.set(j, rsiSeries.get(j - 1)); // 공백 시 이전 값 유지
                        }
                    }
                    break;
                }
            }
        }
        return rsiSeries;
    }

    private BigDecimal calculateRsiValue(BigDecimal avgGain, BigDecimal avgLoss) {
        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.valueOf(100);
        BigDecimal rs = avgGain.divide(avgLoss, MathContext.DECIMAL128);
        return BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), SCALE, ROUNDING));
    }

    private record MacdSeriesResult(BigDecimal macdLine, BigDecimal signalLine) {}

    private List<MacdSeriesResult> calculateMACDSeries(List<BigDecimal> prices) {
        int size = prices.size();
        List<BigDecimal> ema12 = calculateEMASeries(prices, 12);
        List<BigDecimal> ema26 = calculateEMASeries(prices, 26);
        List<BigDecimal> macdLines = new ArrayList<>(Collections.nCopies(size, null));

        for (int i = 0; i < size; i++) {
            if (ema12.get(i) != null && ema26.get(i) != null) {
                // [수정] add(i, val) -> set(i, val)
                macdLines.set(i, ema12.get(i).subtract(ema26.get(i)));
            }
        }

        List<BigDecimal> signalLines = calculateEMASeries(macdLines, 9);
        List<MacdSeriesResult> results = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            results.add(new MacdSeriesResult(
                macdLines.get(i) != null ? macdLines.get(i).setScale(SCALE, ROUNDING) : null,
                signalLines.get(i) != null ? signalLines.get(i).setScale(SCALE, ROUNDING) : null
            ));
        }
        return results;
    }

    private List<BigDecimal> calculateEMASeries(List<BigDecimal> prices, int period) {
        int size = prices.size();
        List<BigDecimal> ema = new ArrayList<>(Collections.nCopies(size, null));
        
        BigDecimal multiplier = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(period + 1), MathContext.DECIMAL128);
        BigDecimal sum = BigDecimal.ZERO;
        int validCount = 0;
        int startIdx = -1;

        for (int i = 0; i < size; i++) {
            if (prices.get(i) != null) {
                sum = sum.add(prices.get(i));
                validCount++;
                if (validCount == period) {
                    ema.set(i, sum.divide(BigDecimal.valueOf(period), SCALE, ROUNDING));
                    startIdx = i;
                    break;
                }
            }
        }

        if (startIdx == -1) return ema;

        for (int i = startIdx + 1; i < size; i++) {
            BigDecimal currentPrice = prices.get(i);
            BigDecimal prevEma = ema.get(i - 1);
            
            if (currentPrice != null && prevEma != null) {
                BigDecimal currentEma = currentPrice.subtract(prevEma).multiply(multiplier).add(prevEma);
                ema.set(i, currentEma);
            } else {
                // [보완] 데이터 공백 시 이전 EMA 유지하여 계산 체인 보존
                ema.set(i, prevEma);
            }
        }
        return ema;
    }

    public TechnicalIndicators calculate(List<BigDecimal> closingPrices) {
        List<TechnicalIndicators> series = calculateSeries(closingPrices);
        return series.isEmpty() ? TechnicalIndicators.empty() : series.get(series.size() - 1);
    }
}
