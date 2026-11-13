package org.stockwellness.application.service.marketdata;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.batch.MarketDerivedDataUseCase;
import org.stockwellness.application.port.out.stock.IndexDailyReadPort;
import org.stockwellness.application.port.out.stock.IndexPriceObservation;
import org.stockwellness.application.port.out.stock.IndexWeatherPort;
import org.stockwellness.application.port.out.stock.PriceObservation;
import org.stockwellness.application.port.out.stock.StockPriceReadPort;
import org.stockwellness.application.port.out.stock.SectorLeadingStockCandidate;
import org.stockwellness.application.port.out.stock.SectorLeadingStockPort;
import org.stockwellness.application.port.out.stock.TechnicalIndicatorPort;
import org.stockwellness.domain.stock.analysis.TechnicalIndicatorCalculator;
import org.stockwellness.domain.stock.insight.IndexTechnicalIndicator;
import org.stockwellness.domain.stock.insight.IndexWeather;
import org.stockwellness.domain.stock.insight.MarketWeatherPolicy;
import org.stockwellness.domain.stock.insight.RollingPercentileCalculator;
import org.stockwellness.domain.stock.price.PriceBasis;
import org.stockwellness.domain.stock.price.StockTechnicalIndicator;
import org.stockwellness.domain.stock.price.TechnicalIndicators;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class MarketDerivedDataService implements MarketDerivedDataUseCase {
    private static final int LOOKBACK = 252;
    private static final int REQUIRED_HISTORY = 120;
    // A 20-session moving average needs five observations before percentile scoring is meaningful.
    private static final int WEATHER_REQUIRED_HISTORY = 24;
    private static final String ALGORITHM_VERSION = "ta4j-eod-v1";
    private static final String WEATHER_VERSION = "market-weather-v1";

    private final StockPriceReadPort stockPriceReadPort;
    private final IndexDailyReadPort indexDailyReadPort;
    private final IndexWeatherPort indexWeatherPort;
    private final TechnicalIndicatorPort technicalIndicatorPort;
    private final SectorLeadingStockPort sectorLeadingStockPort;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void calculateStockIndicators(long stockId, String quoteScope, LocalDate targetDate) {
        if (stockId <= 0 || quoteScope == null || quoteScope.isBlank() || targetDate == null) {
            throw new IllegalArgumentException("Stock, quote scope, and target date are required");
        }
        List<PriceObservation> observations = stockPriceReadPort
                .recent(Set.of(stockId), quoteScope, targetDate, LOOKBACK, PriceBasis.RAW)
                .getOrDefault(stockId, List.of()).stream()
                .sorted(Comparator.comparing(PriceObservation::tradeDate))
                .toList();
        if (observations.size() < REQUIRED_HISTORY || !completeStockHistory(observations, stockId, quoteScope, targetDate)) {
            return;
        }

        List<BigDecimal> highs = observations.stream().map(PriceObservation::highPrice).toList();
        List<BigDecimal> lows = observations.stream().map(PriceObservation::lowPrice).toList();
        List<BigDecimal> closes = observations.stream().map(PriceObservation::closePrice).toList();
        List<LocalDate> dates = observations.stream().map(PriceObservation::tradeDate).toList();
        TechnicalIndicators values = latest(TechnicalIndicatorCalculator.calculateSeries(highs, lows, closes, dates));
        if (values.getMa120() == null) return;

        PriceObservation target = observations.getLast();
        technicalIndicatorPort.upsertStock(new StockTechnicalIndicator(stockId, targetDate, quoteScope,
                PriceBasis.RAW, scale(values.getMa5(), 19, 4), scale(values.getMa20(), 19, 4),
                scale(disparity(target.closePrice(), values.getMa20()), 12, 6), scale(values.getMa60(), 19, 4),
                scale(values.getMa120(), 19, 4), scale(values.getRsi14(), 12, 6),
                scale(values.getMacd(), 19, 6), scale(values.getMacdSignal(), 19, 6),
                scale(values.getAdx(), 12, 6), scale(values.getPlusDi(), 12, 6), scale(values.getMinusDi(), 12, 6),
                manifest("stock_price_daily", stockId, quoteScope, "RAW", targetDate,
                        observations.stream().map(value -> new SourceRevision(value.tradeDate(), value.dataRevision())).toList()),
                ALGORITHM_VERSION, Instant.now()));
    }

    @Override
    @Transactional
    public void calculateIndexIndicators(long seriesId, LocalDate targetDate) {
        if (seriesId <= 0 || targetDate == null) {
            throw new IllegalArgumentException("Index series and target date are required");
        }
        List<IndexPriceObservation> observations = indexDailyReadPort.recentFinal(seriesId, targetDate, LOOKBACK).stream()
                .sorted(Comparator.comparing(IndexPriceObservation::tradeDate))
                .toList();
        if (observations.size() < REQUIRED_HISTORY || !completeIndexHistory(observations, seriesId, targetDate)) return;

        List<BigDecimal> highs = observations.stream().map(IndexPriceObservation::highPrice).toList();
        List<BigDecimal> lows = observations.stream().map(IndexPriceObservation::lowPrice).toList();
        List<BigDecimal> closes = observations.stream().map(IndexPriceObservation::closePrice).toList();
        List<LocalDate> dates = observations.stream().map(IndexPriceObservation::tradeDate).toList();
        TechnicalIndicators values = latest(TechnicalIndicatorCalculator.calculateSeries(highs, lows, closes, dates));
        if (values.getMa120() == null) return;

        IndexPriceObservation target = observations.getLast();
        technicalIndicatorPort.upsertIndex(new IndexTechnicalIndicator(seriesId, targetDate,
                scale(values.getMa5(), 19, 4), scale(values.getMa20(), 19, 4),
                scale(disparity(target.closePrice(), values.getMa20()), 12, 6), scale(values.getMa60(), 19, 4),
                scale(values.getMa120(), 19, 4), scale(values.getRsi14(), 12, 6),
                scale(values.getMacd(), 19, 6), scale(values.getMacdSignal(), 19, 6),
                scale(values.getAdx(), 12, 6), scale(values.getPlusDi(), 12, 6), scale(values.getMinusDi(), 12, 6),
                manifest("index_daily", seriesId, null, null, targetDate,
                        observations.stream().map(value -> new SourceRevision(value.tradeDate(), value.revision())).toList()),
                ALGORITHM_VERSION, Instant.now()));
    }

    @Override
    @Transactional
    public void calculateIndexWeather(long seriesId, LocalDate targetDate) {
        if (seriesId <= 0 || targetDate == null) {
            throw new IllegalArgumentException("Index series and target date are required");
        }
        List<IndexPriceObservation> observations = indexDailyReadPort.recentFinal(seriesId, targetDate, LOOKBACK).stream()
                .sorted(Comparator.comparing(IndexPriceObservation::tradeDate))
                .toList();
        if (observations.size() < WEATHER_REQUIRED_HISTORY || !completeIndexHistory(observations, seriesId, targetDate)) return;

        List<TechnicalIndicators> indicators = TechnicalIndicatorCalculator.calculateSeries(
                observations.stream().map(IndexPriceObservation::highPrice).toList(),
                observations.stream().map(IndexPriceObservation::lowPrice).toList(),
                observations.stream().map(IndexPriceObservation::closePrice).toList(),
                observations.stream().map(IndexPriceObservation::tradeDate).toList());
        List<IndexWeatherInput> history = new ArrayList<>();
        for (int index = 0; index < observations.size(); index++) {
            IndexPriceObservation observation = observations.get(index);
            TechnicalIndicators technical = indicators.get(index);
            if (!hasCompleteBreadth(observation) || technical.getMa20() == null || technical.getRsi14() == null) continue;
            history.add(new IndexWeatherInput(observation, disparity(observation.closePrice(), technical.getMa20()),
                    advanceDeclineRatio(observation), technical.getRsi14()));
        }
        if (history.size() < 5 || !history.getLast().observation().tradeDate().equals(targetDate)) return;

        IndexWeatherInput current = history.getLast();
        int trendScore = RollingPercentileCalculator.calculate(current.ma20Disparity(),
                history.stream().map(IndexWeatherInput::ma20Disparity).toList());
        int breadthScore = RollingPercentileCalculator.calculate(current.advanceDeclineRatio(),
                history.stream().map(IndexWeatherInput::advanceDeclineRatio).toList());
        int momentumScore = RollingPercentileCalculator.calculate(current.rsi14(),
                history.stream().map(IndexWeatherInput::rsi14).toList());
        int score = MarketWeatherPolicy.DEFAULT.calculateIntegratedScore(trendScore, breadthScore, momentumScore);
        List<Map<String, Object>> manifestInputs = history.stream().map(value -> {
            IndexPriceObservation observation = value.observation();
            return Map.<String, Object>of(
                    "date", observation.tradeDate().toString(),
                    "revision", observation.revision(),
                    "advanceCount", observation.advanceCount(),
                    "declineCount", observation.declineCount(),
                    "unchangedCount", observation.unchangedCount(),
                    "upperLimitCount", observation.upperLimitCount(),
                    "lowerLimitCount", observation.lowerLimitCount(),
                    "ma20Disparity", value.ma20Disparity(),
                    "advanceDeclineRatio", value.advanceDeclineRatio(),
                    "rsi14", value.rsi14());
        }).toList();
        Map<String, Object> manifest = Map.of("source", "index_daily", "seriesId", seriesId,
                "targetDate", targetDate.toString(), "indicatorAlgorithm", ALGORITHM_VERSION,
                "scoreVersion", WEATHER_VERSION, "observations", manifestInputs);
        indexWeatherPort.upsert(new IndexWeather(seriesId, targetDate, BigDecimal.valueOf(score).setScale(2),
                "FINAL", WEATHER_VERSION, toJson(manifest, "Could not serialize index weather input manifest"),
                Instant.now()));
    }

    @Override
    @Transactional
    public void calculateSectorLeadingStocks(long seriesId, LocalDate targetDate) {
        if (seriesId <= 0 || targetDate == null) {
            throw new IllegalArgumentException("Index series and target date are required");
        }
        List<SectorLeadingStockCandidate> candidates = sectorLeadingStockPort.finalCandidates(seriesId, targetDate)
                .stream()
                .filter(value -> targetDate.equals(value.priceDate())
                        && "KRX".equals(value.quoteScope())
                        && value.previousClose() != null && value.previousClose().signum() > 0
                        && value.close() != null && value.tradingAmount() != null
                        && value.tradingAmount().signum() >= 0
                        && value.priceRevision() > 0 && value.indexRevision() > 0)
                .toList();
        if (candidates.isEmpty()) return;

        List<SectorLeadingStockCandidate> gainers = candidates.stream()
                .filter(value -> value.close().compareTo(value.previousClose()) > 0)
                .toList();
        List<SectorLeadingStockCandidate> selected = (gainers.isEmpty() ? candidates : gainers).stream()
                .sorted(Comparator.comparing(SectorLeadingStockCandidate::tradingAmount).reversed()
                        .thenComparingLong(SectorLeadingStockCandidate::stockId))
                .limit(5)
                .toList();
        List<SectorLeadingStockPort.RankedSectorStock> ranked = new ArrayList<>();
        List<Map<String, Object>> manifestCandidates = new ArrayList<>();
        for (int index = 0; index < selected.size(); index++) {
            SectorLeadingStockCandidate candidate = selected.get(index);
            int rank = index + 1;
            ranked.add(new SectorLeadingStockPort.RankedSectorStock(
                    candidate.stockId(), candidate.priceDate(), candidate.quoteScope(), rank));
            manifestCandidates.add(Map.of(
                    "rank", rank,
                    "stockId", candidate.stockId(),
                    "priceDate", candidate.priceDate().toString(),
                    "quoteScope", candidate.quoteScope(),
                    "priceRevision", candidate.priceRevision(),
                    "indexRevision", candidate.indexRevision()));
        }
        Map<String, Object> manifest = Map.of(
                "source", "stock_price_daily",
                "indexSeriesId", seriesId,
                "tradeDate", targetDate.toString(),
                "ruleVersion", "sector-leading-turnover-gainers-v1",
                "selected", manifestCandidates);
        sectorLeadingStockPort.replace(seriesId, targetDate, ranked,
                toJson(manifest, "Could not serialize sector leading stock input manifest"));
    }

    private static boolean completeStockHistory(List<PriceObservation> values, long stockId,
            String quoteScope, LocalDate targetDate) {
        return values.getLast().tradeDate().equals(targetDate)
                && values.stream().allMatch(value -> value.stockId() == stockId
                    && quoteScope.equals(value.quoteScope()) && value.priceBasis() == PriceBasis.RAW
                    && "FINAL".equals(value.dataStatus()) && value.tradeDate() != null
                    && !value.tradeDate().isAfter(targetDate) && value.dataRevision() != null
                    && validOhlc(value.openPrice(), value.highPrice(), value.lowPrice(), value.closePrice()));
    }

    private static boolean completeIndexHistory(List<IndexPriceObservation> values, long seriesId, LocalDate targetDate) {
        return values.getLast().tradeDate().equals(targetDate)
                && values.stream().allMatch(value -> value.seriesId() == seriesId && "FINAL".equals(value.dataStatus())
                    && value.tradeDate() != null && !value.tradeDate().isAfter(targetDate) && value.revision() > 0
                    && validOhlc(value.highPrice(), value.lowPrice(), value.closePrice()));
    }

    private static boolean validOhlc(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {
        return positive(open) && positive(high) && positive(low) && positive(close)
                && high.compareTo(low) >= 0 && high.compareTo(open) >= 0 && high.compareTo(close) >= 0
                && low.compareTo(open) <= 0 && low.compareTo(close) <= 0;
    }

    private static boolean validOhlc(BigDecimal high, BigDecimal low, BigDecimal close) {
        return positive(high) && positive(low) && positive(close)
                && high.compareTo(low) >= 0 && high.compareTo(close) >= 0 && low.compareTo(close) <= 0;
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static boolean hasCompleteBreadth(IndexPriceObservation value) {
        return value.advanceCount() != null && value.advanceCount() >= 0
                && value.declineCount() != null && value.declineCount() >= 0
                && value.unchangedCount() != null && value.unchangedCount() >= 0
                && value.upperLimitCount() != null && value.upperLimitCount() >= 0
                && value.lowerLimitCount() != null && value.lowerLimitCount() >= 0
                && (long) value.advanceCount() + value.declineCount() + value.unchangedCount()
                        + value.upperLimitCount() + value.lowerLimitCount() > 0;
    }

    private static BigDecimal advanceDeclineRatio(IndexPriceObservation value) {
        long advancing = (long) value.advanceCount() + value.upperLimitCount();
        long declining = (long) value.declineCount() + value.lowerLimitCount();
        if (advancing == 0 && declining == 0) return BigDecimal.valueOf(100);
        return BigDecimal.valueOf(advancing).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(Math.max(declining, 1)), 2, RoundingMode.HALF_UP);
    }

    private String manifest(String source, long sourceId, String quoteScope, String priceBasis,
            LocalDate targetDate, List<SourceRevision> revisions) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("source", source);
        manifest.put("sourceId", sourceId);
        if (quoteScope != null) manifest.put("quoteScope", quoteScope);
        if (priceBasis != null) manifest.put("priceBasis", priceBasis);
        manifest.put("targetDate", targetDate.toString());
        manifest.put("observations", revisions.stream()
                .map(value -> Map.of("date", value.date().toString(), "revision", value.revision()))
                .toList());
        return toJson(manifest, "Could not serialize technical indicator input manifest");
    }

    private String toJson(Object value, String message) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(message, ex);
        }
    }

    private static TechnicalIndicators latest(List<TechnicalIndicators> values) {
        return values == null || values.isEmpty() ? TechnicalIndicators.empty() : values.getLast();
    }

    private static BigDecimal disparity(BigDecimal close, BigDecimal ma20) {
        return close == null || ma20 == null || ma20.signum() == 0
                ? null : close.divide(ma20, 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale(BigDecimal value, int precision, int scale) {
        if (value == null) return null;
        BigDecimal result = value.setScale(scale, RoundingMode.HALF_UP);
        if (result.precision() > precision) throw new IllegalArgumentException("Technical indicator exceeds storage range");
        return result;
    }

    private record SourceRevision(LocalDate date, Integer revision) {}

    private record IndexWeatherInput(IndexPriceObservation observation, BigDecimal ma20Disparity,
            BigDecimal advanceDeclineRatio, BigDecimal rsi14) {}
}
