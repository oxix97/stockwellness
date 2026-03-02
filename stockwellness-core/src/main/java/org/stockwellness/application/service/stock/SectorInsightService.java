package org.stockwellness.application.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.stock.SectorInsightUseCase;
import org.stockwellness.application.port.in.stock.result.SectorComparisonResult;
import org.stockwellness.application.port.in.stock.result.SectorDetailResult;
import org.stockwellness.application.port.in.stock.result.SectorRankingResult;
import org.stockwellness.application.port.in.stock.result.SectorSupplyResult;
import org.stockwellness.application.port.out.stock.*;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.domain.stock.insight.exception.SectorDomainException;
import org.stockwellness.domain.stock.analysis.DiagnosisStatus;
import org.stockwellness.domain.stock.price.TechnicalIndicators;
import org.stockwellness.global.error.ErrorCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorInsightService implements SectorInsightUseCase {

    private final SectorInsightPort sectorInsightPort;

    @Override
    @Transactional
    public void syncAllSectorInsights() {
        log.info("섹터 동기화는 이제 Batch Job(sectorEodJob)을 통해 관리됩니다. 이 메서드는 더 이상 사용되지 않습니다.");
    }

    @Override
    @Cacheable(cacheNames = "sectorRanking", key = "#date.toString() + '_' + (#marketType != null ? #marketType.name() : 'ALL') + '_' + #limit")
    public List<SectorRankingResult> getTopSectorsByFluctuation(LocalDate date, MarketType marketType, int limit) {
        return sectorInsightPort.findTopSectorsByFluctuation(date, marketType, limit).stream()
                .map(s -> new SectorRankingResult(s.getSectorCode(), s.getSectorName(), s.getSectorIndexCurrentPrice(), s.getAvgFluctuationRate(), s.isOverheated()))
                .toList();
    }

    @Override
    @Cacheable(cacheNames = "sectorSupply", key = "#date.toString() + '_' + (#marketType != null ? #marketType.name() : 'ALL') + '_' + #limit")
    public List<SectorSupplyResult> getTopSectorsBySupply(LocalDate date, MarketType marketType, int limit) {
        return sectorInsightPort.findTopSectorsBySupply(date, marketType, limit).stream()
                .map(s -> new SectorSupplyResult(s.getSectorCode(), s.getSectorName(), s.getNetForeignBuyAmount(), s.getNetInstBuyAmount(), s.getForeignConsecutiveBuyDays(), s.getInstConsecutiveBuyDays()))
                .toList();
    }

    @Override
    @Cacheable(cacheNames = "sectorDetail", key = "#sectorCode + '_' + #date.toString()")
    public SectorDetailResult getSectorDetail(String sectorCode, LocalDate date) {
        SectorInsight insight = sectorInsightPort.findBySectorCodeAndDate(sectorCode, date)
                .orElseThrow(() -> new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND));

        return new SectorDetailResult(
                insight.getSectorCode(), insight.getSectorName(), insight.getBaseDate(),
                insight.getSectorIndexCurrentPrice(), insight.getAvgFluctuationRate(),
                insight.getTechnicalIndicators(), insight.isOverheated(),
                generateDiagnosisMessage(insight), insight.getLeadingStocks()
        );
    }

    @Override
    @Cacheable(cacheNames = "sectorComparison", key = "#sectorCode + '_' + #date.toString()")
    public SectorComparisonResult compareWithMarket(String sectorCode, LocalDate date) {
        SectorInsight sector = sectorInsightPort.findBySectorCodeAndDate(sectorCode, date)
                .orElseThrow(() -> new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND));

        String marketCode = (sector.getMarketType() == MarketType.KOSDAQ) ? "1001" : "0001";
        Map<String, SectorInsight> todayData = sectorInsightPort.findByCodesAndDate(List.of(sectorCode, marketCode), date).stream()
                .collect(Collectors.toMap(SectorInsight::getSectorCode, s -> s));

        SectorInsight market = todayData.get(marketCode);
        if (market == null) throw new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND);

        BigDecimal rs = sector.getAvgFluctuationRate().subtract(market.getAvgFluctuationRate());
        List<SectorInsight> sectorHistory = sectorInsightPort.findHistoryByCode(sectorCode, date, 5);
        List<SectorInsight> marketHistory = sectorInsightPort.findHistoryByCode(marketCode, date, 5);
        
        List<SectorComparisonResult.HistoricalRS> history = calculateHistoricalRS(sectorHistory, marketHistory);

        return new SectorComparisonResult(
                sectorCode, sector.getSectorName(), date,
                sector.getAvgFluctuationRate(), market.getAvgFluctuationRate(),
                rs, resolvePerformanceStatus(rs), history
        );
    }

    private List<SectorComparisonResult.HistoricalRS> calculateHistoricalRS(List<SectorInsight> sectorHistory, List<SectorInsight> marketHistory) {
        Map<LocalDate, BigDecimal> marketMap = marketHistory.stream()
                .collect(Collectors.toMap(SectorInsight::getBaseDate, SectorInsight::getAvgFluctuationRate));

        return sectorHistory.stream()
                .map(s -> {
                    BigDecimal mRate = marketMap.get(s.getBaseDate());
                    BigDecimal rs = (mRate != null) ? s.getAvgFluctuationRate().subtract(mRate) : BigDecimal.ZERO;
                    return new SectorComparisonResult.HistoricalRS(s.getBaseDate(), rs);
                })
                .sorted(Comparator.comparing(SectorComparisonResult.HistoricalRS::date))
                .toList();
    }

    private String resolvePerformanceStatus(BigDecimal rs) {
        if (rs.compareTo(new BigDecimal("0.5")) > 0) return "OUTPERFORM";
        if (rs.compareTo(new BigDecimal("-0.5")) < 0) return "UNDERPERFORM";
        return "NEUTRAL";
    }

    private String generateDiagnosisMessage(SectorInsight insight) {
        TechnicalIndicators indicators = insight.getTechnicalIndicators();
        if (indicators == null || indicators.getRsi14() == null) return DiagnosisStatus.DATA_INSUFFICIENT.getMessage();

        boolean rsiOver = indicators.getRsi14().compareTo(new BigDecimal("70")) > 0;
        boolean rsiUnder = indicators.getRsi14().compareTo(new BigDecimal("30")) < 0;
        
        BigDecimal disparity = BigDecimal.ZERO;
        if (indicators.getMa20() != null && indicators.getMa20().compareTo(BigDecimal.ZERO) > 0) {
            disparity = insight.getSectorIndexCurrentPrice().divide(indicators.getMa20(), 4, java.math.RoundingMode.HALF_UP);
        }
        boolean disparityOver = disparity.compareTo(new BigDecimal("1.1")) >= 0;

        if (rsiOver && disparityOver) return DiagnosisStatus.OVERHEATED_BOTH.getMessage();
        if (rsiOver) return DiagnosisStatus.OVERHEATED_RSI.getMessage();
        if (disparityOver) return DiagnosisStatus.OVERHEATED_DISPARITY.getMessage();
        if (rsiUnder) return DiagnosisStatus.STAGNANT.getMessage();
        
        return DiagnosisStatus.NORMAL.getMessage();
    }
}
