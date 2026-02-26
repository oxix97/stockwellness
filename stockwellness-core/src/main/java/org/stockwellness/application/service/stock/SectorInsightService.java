package org.stockwellness.application.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.external.kis.dto.InvestorTradingDaily;
import org.stockwellness.adapter.out.external.kis.dto.KisDailySectorDetail;
import org.stockwellness.application.port.in.stock.SectorInsightUseCase;
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
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;
import org.stockwellness.global.error.ErrorCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

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
        
        if (indices.isEmpty()) {
            log.warn("동기화할 업종 지수(MarketIndex) 데이터가 없습니다.");
            return;
        }

        LocalDate baseDate = LocalDate.now();

        for (MarketIndex index : indices) {
            try {
                syncSectorInsight(index, baseDate);
            } catch (SectorDomainException e) {
                log.error("업종 데이터 부족으로 동기화 건너뜜 - {}({}): {}", 
                    index.getIndexName(), index.getIndexCode(), e.getMessage());
            } catch (Exception e) {
                log.error("업종 동기화 중 예상치 못한 오류 발생 - {}({}): {}", 
                    index.getIndexName(), index.getIndexCode(), e.getMessage(), e);
            }
        }
    }

    @Override
    @Cacheable(cacheNames = "sectorRanking", key = "#date.toString() + '_' + #limit")
    public List<SectorRankingResult> getTopSectorsByFluctuation(LocalDate date, int limit) {
        return sectorInsightPort.findTopSectorsByFluctuation(date, limit).stream()
                .map(s -> new SectorRankingResult(
                        s.getSectorCode(),
                        s.getSectorName(),
                        s.getSectorIndexCurrentPrice(),
                        s.getAvgFluctuationRate(),
                        s.isOverheated()
                ))
                .toList();
    }

    @Override
    @Cacheable(cacheNames = "sectorSupply", key = "#date.toString() + '_' + #limit")
    public List<SectorSupplyResult> getTopSectorsBySupply(LocalDate date, int limit) {
        return sectorInsightPort.findTopSectorsBySupply(date, limit).stream()
                .map(s -> new SectorSupplyResult(
                        s.getSectorCode(),
                        s.getSectorName(),
                        s.getNetForeignBuyAmount(),
                        s.getNetInstBuyAmount(),
                        s.getForeignConsecutiveBuyDays(),
                        s.getInstConsecutiveBuyDays()
                ))
                .toList();
    }

    @Override
    @Cacheable(cacheNames = "sectorDetail", key = "#sectorCode + '_' + #date.toString()")
    public SectorDetailResult getSectorDetail(String sectorCode, LocalDate date) {
        SectorInsight insight = sectorInsightPort.findBySectorCodeAndDate(sectorCode, date)
                .orElseThrow(() -> new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND));

        return new SectorDetailResult(
                insight.getSectorCode(),
                insight.getSectorName(),
                insight.getBaseDate(),
                insight.getSectorIndexCurrentPrice(),
                insight.getAvgFluctuationRate(),
                insight.getTechnicalIndicators(),
                insight.isOverheated(),
                generateDiagnosisMessage(insight),
                insight.getLeadingStocks()
        );
    }

    private String generateDiagnosisMessage(SectorInsight insight) {
        TechnicalIndicators indicators = insight.getTechnicalIndicators();
        StringBuilder sb = new StringBuilder();
        
        if (insight.isOverheated()) {
            sb.append("현재 섹터는 과열 구간에 진입했습니다. ");
        }
        
        sb.append(TechnicalCalculator.analyzeRsiLevel(indicators.getRsi14()));
        
        return sb.toString();
    }

    private void syncSectorInsight(MarketIndex index, LocalDate baseDate) {
        String indexCode = index.getIndexCode();
        
        // 1. 외부 API 데이터 조회
        KisDailySectorDetail detail = sectorDataPort.fetchDailySectorDetail(indexCode);
        List<InvestorTradingDaily> investorData = sectorDataPort.fetchInvestorTradingDaily(indexCode, 10);
        List<BigDecimal> historicalPrices = sectorDataPort.fetchHistoricalIndexPrices(indexCode, 120);

        InvestorTradingDaily todayInvestor = investorData.get(0);
        BigDecimal currentPrice = new BigDecimal(detail.sectorIndexPrice());
        
        // 2. 수급 지표 계산
        int foreignConsecutiveDays = calculateConsecutiveDays(indexCode, baseDate, parseLong(todayInvestor.frgnNtbyTrPbmn()), true);
        int instConsecutiveDays = calculateConsecutiveDays(indexCode, baseDate, parseLong(todayInvestor.orgnNtbyTrPbmn()), false);

        // 3. 기술적 지표 계산 및 과열 진단
        TechnicalIndicators indicators = TechnicalIndicatorCalculator.calculateLatest(historicalPrices);
        boolean isOverheated = TechnicalCalculator.isOverheated(
                currentPrice, 
                indicators.getMa20(), 
                indicators.getRsi14()
        );

        // 4. 도메인 엔티티 생성
        SectorInsight insight = SectorInsight.of(
            index.getIndexName(),
            indexCode,
            resolveMarketType(indexCode),
            baseDate,
            currentPrice,
            new BigDecimal(detail.sectorIndexPriceChangeRate()),
            parseLong(todayInvestor.frgnNtbyTrPbmn()),
            parseLong(todayInvestor.orgnNtbyTrPbmn()),
            foreignConsecutiveDays,
            instConsecutiveDays,
            indicators,
            isOverheated
        );

        // 5. 주도주 추출
        List<Stock> stocksInSector = stockPort.findBySectorMediumName(indexCode);
        if (!stocksInSector.isEmpty()) {
            List<StockPrice> prices = stockPricePort.findByStocksAndDate(stocksInSector, baseDate);
            List<LeadingStock> leadingStocks = prices.stream()
                .filter(p -> p.getTransactionAmount() != null)
                .filter(p -> p.getFluctuationRate().compareTo(BigDecimal.valueOf(3.0)) >= 0)
                .sorted(Comparator.comparing(StockPrice::getTransactionAmount).reversed())
                .limit(5)
                .map(LeadingStock::from)
                .toList();
            insight.updateLeadingStocks(leadingStocks);
        }

        // 6. 저장
        sectorInsightPort.save(insight);
    }

    private int calculateConsecutiveDays(String sectorCode, LocalDate baseDate, long currentNetBuy, boolean isForeign) {
        if (currentNetBuy <= 0) return 0;

        return sectorInsightPort.findLatestBefore(sectorCode, baseDate)
            .map(prev -> {
                int prevDays = isForeign ? prev.getForeignConsecutiveBuyDays() : prev.getInstConsecutiveBuyDays();
                return prevDays + 1;
            })
            .orElse(1);
    }

    private MarketType resolveMarketType(String indexCode) {
        if (indexCode.startsWith("0")) return MarketType.KOSPI;
        if (indexCode.startsWith("1")) return MarketType.KOSDAQ;
        return MarketType.KOSPI;
    }

    private Long parseLong(String val) {
        if (val == null || val.isBlank()) return 0L;
        try {
            return Long.parseLong(val.replaceAll(",", ""));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
