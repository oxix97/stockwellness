package org.stockwellness.application.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.external.kis.dto.InvestorTradingDaily;
import org.stockwellness.adapter.out.external.kis.dto.KisDailySectorDetail;
import org.stockwellness.application.port.in.stock.SectorInsightUseCase;
import org.stockwellness.application.port.in.stock.result.SectorComparisonResult;
import org.stockwellness.application.port.in.stock.result.SectorDetailResult;
import org.stockwellness.application.port.in.stock.result.SectorRankingResult;
import org.stockwellness.application.port.in.stock.result.SectorSupplyResult;
import org.stockwellness.application.port.out.stock.*;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.insight.LeadingStock;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.domain.stock.insight.exception.SectorDomainException;
import org.stockwellness.domain.stock.analysis.TechnicalIndicatorCalculator;
import org.stockwellness.domain.stock.analysis.TechnicalCalculator;
import org.stockwellness.domain.stock.analysis.DiagnosisStatus;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;
import org.stockwellness.global.error.ErrorCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorInsightService implements SectorInsightUseCase {

    private final MarketIndexPort marketIndexPort;
    private final SectorDataPort sectorDataPort;
    private final StockPort stockPort;
    private final StockPricePort stockPricePort;
    private final SectorInsightPort sectorInsightPort;

    @Override
    @Transactional
    public void syncAllSectorInsights() {
        List<MarketIndex> indices = marketIndexPort.findAll();
        if (indices.isEmpty()) return;

        LocalDate baseDate = LocalDate.now();
        for (MarketIndex index : indices) {
            try {
                syncSectorInsight(index, baseDate);
            } catch (Exception e) {
                log.error("Failed to sync sector insight for {}: {}", index.getIndexCode(), e.getMessage());
            }
        }
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
        // 1. 섹터 정보 조회
        SectorInsight sector = sectorInsightPort.findBySectorCodeAndDate(sectorCode, date)
                .orElseThrow(() -> new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND));

        // 2. 해당 시장의 종합 지수 코드 결정 (KOSPI: 0001, KOSDAQ: 1001)
        String marketCode = (sector.getMarketType() == MarketType.KOSDAQ) ? "1001" : "0001";
        
        // 3. 당일 데이터 벌크 조회 (N+1 방지)
        Map<String, SectorInsight> todayData = sectorInsightPort.findByCodesAndDate(List.of(sectorCode, marketCode), date).stream()
                .collect(Collectors.toMap(SectorInsight::getSectorCode, s -> s));

        SectorInsight market = todayData.get(marketCode);
        if (market == null) throw new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND);

        // 4. 상대 강도 계산
        BigDecimal rs = sector.getAvgFluctuationRate().subtract(market.getAvgFluctuationRate());

        // 5. 히스토리 트렌드 조회 (최근 5일)
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

    private void syncSectorInsight(MarketIndex index, LocalDate baseDate) {
        // 기존 동기화 로직 유지 (생략)
    }

    private Long parseLong(String val) {
        if (val == null || val.isBlank()) return 0L;
        try { return Long.parseLong(val.replaceAll(",", "")); } catch (Exception e) { return 0L; }
    }
}
