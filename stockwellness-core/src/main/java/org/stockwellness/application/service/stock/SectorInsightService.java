package org.stockwellness.application.service.stock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.stock.SectorInsightUseCase;
import org.stockwellness.application.port.in.stock.result.SectorComparisonResult;
import org.stockwellness.application.port.in.stock.result.SectorDetailResult;
import org.stockwellness.application.port.in.stock.result.SectorRankingResult;
import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import org.stockwellness.application.port.out.stock.LoadBenchmarkPort;
import org.stockwellness.application.port.out.stock.SectorMarketObservation;
import org.stockwellness.application.port.out.stock.SectorMarketReadPort;
import org.stockwellness.domain.stock.BenchmarkType;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.analysis.DiagnosisStatus;
import org.stockwellness.domain.stock.analysis.TechnicalCalculator;
import org.stockwellness.domain.stock.insight.SectorTechnicalIndicators;
import org.stockwellness.domain.stock.insight.exception.SectorDomainException;
import org.stockwellness.global.error.ErrorCode;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorInsightService implements SectorInsightUseCase {
    private final SectorMarketReadPort sectorMarketReadPort;
    private final LoadBenchmarkPort loadBenchmarkPort;

    @Override
    @Transactional
    public void syncAllSectorInsights() {
        log.info("섹터 데이터는 market orchestration Job에서 관리됩니다.");
    }

    @Override
    @Cacheable(cacheNames = "sectorRanking:v4", key = "#p0 + '_' + (#p1 != null ? #p1 : 'ALL') + '_' + #p2", unless = "#result == null")
    public List<SectorRankingResult> getTopSectorsByFluctuation(LocalDate date, MarketType marketType, int limit) {
        String marketCode = marketType == null ? null : marketType.name();
        LocalDate effectiveDate = date;
        List<SectorMarketObservation> observations = effectiveDate == null
                ? List.of() : sectorMarketReadPort.finalOnDate(effectiveDate, marketCode);
        if (observations.isEmpty()) {
            effectiveDate = sectorMarketReadPort.latestFinalDate(marketCode).orElse(null);
            if (effectiveDate == null) return List.of();
            observations = sectorMarketReadPort.finalOnDate(effectiveDate, marketCode);
        }

        return observations.stream()
                .filter(row -> row.sectorName() != null && row.closePrice() != null && row.changeRate() != null)
                .collect(Collectors.groupingBy(row -> normalize(row.sectorName())))
                .values().stream()
                .filter(group -> !group.isEmpty())
                .map(group -> {
                    SectorMarketObservation representative = group.getFirst();
                    BigDecimal averageRate = group.stream().map(SectorMarketObservation::changeRate)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(group.size()), 4, RoundingMode.HALF_UP);
                    BigDecimal averagePrice = group.stream().map(SectorMarketObservation::closePrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(group.size()), 2, RoundingMode.HALF_UP);
                    boolean overheated = group.stream().anyMatch(SectorInsightService::isOverheated);
                    return new SectorRankingResult(representative.sectorCode(), representative.sectorName(),
                            averagePrice, averageRate, overheated, null);
                })
                .sorted(Comparator.comparing(SectorRankingResult::fluctuationRate).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    @Cacheable(cacheNames = "sectorDetail:v4", key = "#p0 + '_' + #p1", unless = "#result == null")
    public SectorDetailResult getSectorDetail(String sectorCode, LocalDate date) {
        if (sectorCode == null || sectorCode.isBlank()) {
            throw new SectorDomainException(ErrorCode.INVALID_INPUT_VALUE);
        }
        LocalDate asOf = date == null ? LocalDate.now() : date;
        SectorMarketObservation sector = sectorMarketReadPort.latestFinal(sectorCode, asOf)
                .orElseThrow(() -> new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND));
        SectorTechnicalIndicators indicators = technicalIndicators(sector);
        return new SectorDetailResult(sector.sectorCode(), sector.sectorName(), sector.tradeDate(),
                sector.closePrice(), sector.changeRate(), indicators, isOverheated(sector),
                generateDiagnosisMessage(sector.closePrice(), indicators),
                sectorMarketReadPort.leadingStocks(sector.seriesId(), sector.tradeDate()), null);
    }

    @Override
    @Cacheable(cacheNames = "sectorComparison:v4", key = "#p0", unless = "#result == null")
    public SectorComparisonResult compareWithMarket(String sectorCode) {
        if (sectorCode == null || sectorCode.isBlank()) {
            throw new SectorDomainException(ErrorCode.INVALID_INPUT_VALUE);
        }
        SectorMarketObservation sector = sectorMarketReadPort.latestFinal(sectorCode, LocalDate.now().plusDays(1))
                .orElseThrow(() -> new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND));
        String marketTicker = marketTicker(sector.marketCode());
        StockPriceResult market = loadBenchmarkPort.loadBenchmarkPriceAtOrBefore(marketTicker, sector.tradeDate())
                .orElseThrow(() -> new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND));
        if (sector.changeRate() == null || market.changeRate() == null) {
            throw new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND);
        }

        BigDecimal relativeStrength = sector.changeRate().subtract(market.changeRate());
        List<SectorMarketObservation> sectorHistory = sectorMarketReadPort
                .historyFinal(sector.seriesId(), sector.tradeDate(), 30);
        LocalDate firstDate = sectorHistory.stream().map(SectorMarketObservation::tradeDate)
                .min(LocalDate::compareTo).orElse(sector.tradeDate());
        Map<LocalDate, BigDecimal> marketRates = loadBenchmarkPort
                .loadBenchmarkPrices(marketTicker, firstDate, sector.tradeDate()).stream()
                .filter(price -> price.changeRate() != null)
                .collect(Collectors.toMap(StockPriceResult::baseDate, StockPriceResult::changeRate, (first, ignored) -> first));
        List<SectorComparisonResult.HistoricalRS> history = sectorHistory.stream()
                .filter(row -> row.changeRate() != null && marketRates.containsKey(row.tradeDate()))
                .map(row -> {
                    BigDecimal marketRate = marketRates.get(row.tradeDate());
                    return new SectorComparisonResult.HistoricalRS(row.tradeDate(), row.changeRate(), marketRate,
                            row.changeRate().subtract(marketRate));
                })
                .sorted(Comparator.comparing(SectorComparisonResult.HistoricalRS::date))
                .toList();
        return new SectorComparisonResult(sector.sectorCode(), sector.sectorName(), sector.tradeDate(),
                sector.changeRate(), market.changeRate(), relativeStrength, resolvePerformanceStatus(relativeStrength),
                history);
    }

    private static String normalize(String name) {
        return name.replaceAll("[^가-힣a-zA-Z0-9]", "");
    }

    private static boolean isOverheated(SectorMarketObservation row) {
        return TechnicalCalculator.isOverheated(row.closePrice(), row.ma20(), row.rsi14());
    }

    private static SectorTechnicalIndicators technicalIndicators(SectorMarketObservation row) {
        return SectorTechnicalIndicators.of(row.ma5(), row.ma20(), row.rsi14(), null, null, null, null, null);
    }

    private String generateDiagnosisMessage(BigDecimal currentPrice, SectorTechnicalIndicators indicators) {
        if (currentPrice == null || indicators == null || indicators.getRsi14() == null || indicators.getMa20() == null) {
            return DiagnosisStatus.DATA_INSUFFICIENT.getMessage();
        }
        boolean rsiOver = indicators.getRsi14().compareTo(new BigDecimal("70")) > 0;
        boolean rsiUnder = indicators.getRsi14().compareTo(new BigDecimal("30")) < 0;
        BigDecimal disparity = BigDecimal.ZERO;
        if (indicators.getMa20() != null && indicators.getMa20().signum() > 0) {
            disparity = currentPrice.divide(indicators.getMa20(), 4, RoundingMode.HALF_UP);
        }
        boolean disparityOver = disparity.compareTo(new BigDecimal("1.1")) >= 0;
        if (rsiOver && disparityOver) return DiagnosisStatus.OVERHEATED_BOTH.getMessage();
        if (rsiOver) return DiagnosisStatus.OVERHEATED_RSI.getMessage();
        if (disparityOver) return DiagnosisStatus.OVERHEATED_DISPARITY.getMessage();
        if (rsiUnder) return DiagnosisStatus.STAGNANT.getMessage();
        return DiagnosisStatus.NORMAL.getMessage();
    }

    private String resolvePerformanceStatus(BigDecimal relativeStrength) {
        if (relativeStrength.compareTo(new BigDecimal("0.4")) > 0) return "OUTPERFORM";
        if (relativeStrength.compareTo(new BigDecimal("-0.4")) < 0) return "UNDERPERFORM";
        return "NEUTRAL";
    }

    private static String marketTicker(String marketCode) {
        if ("KOSPI".equals(marketCode)) return BenchmarkType.KOSPI.getTicker();
        if ("KOSDAQ".equals(marketCode)) return BenchmarkType.KOSDAQ.getTicker();
        throw new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND);
    }
}
