package org.stockwellness.batch.job.stock.sector.job.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.external.kis.dto.InvestorTradingDaily;
import org.stockwellness.application.port.out.stock.*;
import org.stockwellness.application.service.stock.SectorAnalysisService;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.insight.LeadingStock;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.domain.stock.price.StockPrice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SectorInsightItemProcessor implements ItemProcessor<LocalDate, List<SectorInsight>> {

    private final MarketIndexPort marketIndexPort;
    private final SectorInsightPort sectorInsightPort;
    private final SectorDataPort sectorDataPort;
    private final StockPort stockPort;
    private final StockPricePort stockPricePort;
    private final SectorAnalysisService sectorAnalysisService;

    @Override
    public List<SectorInsight> process(LocalDate targetDate) {
        log.info("Analyzing Sector Insights for date: {}", targetDate);

        // 1. 기초 데이터 확보
        List<MarketIndex> indices = marketIndexPort.findAll();
        List<String> sectorCodes = indices.stream().map(MarketIndex::getIndexCode).toList();
        
        // [Pre-fetching] 해당 날짜의 모든 종목 시세 로드 (N+1 방지)
        List<StockPrice> allPrices = stockPricePort.findByStocksAndDate(Collections.emptyList(), targetDate);
        if (allPrices.isEmpty()) {
            log.warn("날짜 {}에 대한 종목 시세 데이터가 없어 섹터 분석을 건너뜁니다.", targetDate);
            return null;
        }

        Map<String, StockPrice> priceMap = allPrices.stream()
                .collect(Collectors.toMap(p -> p.getStock().getTicker(), p -> p, (p1, p2) -> p1));

        Map<String, List<Stock>> sectorToStocksMap = stockPort.findBySectorMediumName(null).stream()
                .filter(s -> s.getSectorMediumName() != null)
                .collect(Collectors.groupingBy(Stock::getSectorMediumName));

        // 2. [Pre-fetching] 모든 섹터의 전일 데이터 및 과거 시계열 로드 (N+1 방지)
        Map<String, SectorInsight> yesterdayMap = sectorInsightPort.findLatestBeforeByCodes(sectorCodes, targetDate);
        Map<String, List<BigDecimal>> pastPricesMap = sectorInsightPort.findPastPricesByCodes(sectorCodes, targetDate, 119);

        List<SectorInsight> results = new ArrayList<>();
        for (MarketIndex index : indices) {
            try {
                String code = index.getIndexCode();
                SectorInsight insight = analyzeSingleSector(
                        index, targetDate, priceMap, 
                        sectorToStocksMap.get(code), 
                        yesterdayMap.get(code), 
                        pastPricesMap.getOrDefault(code, Collections.emptyList())
                );
                
                if (insight != null) {
                    results.add(insight);
                    // [안정성] API Rate Limit 대응
                    Thread.sleep(70);
                }
            } catch (Exception e) {
                log.error("섹터 {} 분석 중 오류 발생: {}", index.getIndexCode(), e.getMessage());
            }
        }
        return results;
    }

    private SectorInsight analyzeSingleSector(MarketIndex index, LocalDate date, Map<String, StockPrice> priceMap, 
                                            List<Stock> stocks, SectorInsight yesterdayData, List<BigDecimal> pastPrices) {
        if (stocks == null || stocks.isEmpty()) return null;

        String sectorCode = index.getIndexCode();
        List<StockPrice> sectorPrices = stocks.stream()
                .map(s -> priceMap.get(s.getTicker()))
                .filter(Objects::nonNull)
                .toList();

        if (sectorPrices.isEmpty()) return null;

        // 1. 외부 데이터 확보 (API 호출 - Rate Limit 주의)
        Long netForeign = 0L;
        Long netInst = 0L;
        BigDecimal currentSectorPrice = BigDecimal.ZERO;
        try {
            var detail = sectorDataPort.fetchDailySectorDetail(sectorCode, date);
            currentSectorPrice = new BigDecimal(detail.sectorIndexPrice());
            
            List<InvestorTradingDaily> investorData = sectorDataPort.fetchInvestorTradingDaily(sectorCode, date, 1);
            if (!investorData.isEmpty()) {
                netForeign = parseLong(investorData.get(0).frgnNtbyTrPbmn());
                netInst = parseLong(investorData.get(0).orgnNtbyTrPbmn());
            }
        } catch (Exception e) { 
            log.debug("외부 API 데이터 누락 - 섹터 {}: {}", sectorCode, e.getMessage());
        }

        // 2. 도메인 서비스로 분석 위임
        SectorApiDto apiDto = new SectorApiDto(
                sectorCode, index.getIndexName(), date, currentSectorPrice,
                calculateAvgFluctuation(sectorPrices), netForeign, netInst
        );

        SectorInsight insight = sectorAnalysisService.analyze(index, apiDto, yesterdayData, pastPrices);

        // 3. 주도주 추출 (상승 종목 중 거래대금 상위 5개)
        List<LeadingStock> leading = sectorPrices.stream()
                .filter(p -> p.getFluctuationRate().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(StockPrice::getTransactionAmount).reversed())
                .limit(5)
                .map(LeadingStock::from)
                .toList();
        insight.updateLeadingStocks(leading);

        return insight;
    }

    private BigDecimal calculateAvgFluctuation(List<StockPrice> prices) {
        return prices.stream()
                .map(StockPrice::getFluctuationRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(prices.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private Long parseLong(String val) {
        if (val == null || val.isBlank()) return 0L;
        try { return Long.parseLong(val.replaceAll(",", "")); } catch (Exception e) { return 0L; }
    }
}
