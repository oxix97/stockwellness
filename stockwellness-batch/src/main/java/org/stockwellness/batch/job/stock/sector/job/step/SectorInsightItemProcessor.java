package org.stockwellness.batch.job.stock.sector.job.step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.*;
import org.stockwellness.application.service.stock.SectorAnalysisService;
import org.stockwellness.domain.stock.Stock;
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
        log.info("Analyzing Sector Insights (DB-based) for date: {}", targetDate);

        // 1. 기초 데이터 확보
        List<MarketIndex> indices = marketIndexPort.findAll();
        List<String> sectorCodes = indices.stream().map(MarketIndex::getIndexCode).toList();

        // [Pre-fetching] 해당 날짜의 모든 종목 시세 로드
        List<StockPrice> allPrices = stockPricePort.findAllByDate(targetDate);
        if (allPrices.isEmpty()) {
            log.warn("날짜 {}에 대한 종목 시세 데이터가 없어 섹터 분석을 건너뜁니다.", targetDate);
            return null;
        }

        // 티커별 시세 맵
        Map<String, StockPrice> priceMap = allPrices.stream()
                .collect(Collectors.toMap(p -> p.getStock().getTicker(), p -> p, (p1, p2) -> p1));

        // [Mapping] 모든 종목 로드 및 섹터 코드별 매핑
        List<Stock> allStocks = stockPort.findBysectorMediumCode(null);
        Map<String, List<Stock>> sectorToStocksMap = new HashMap<>();

        for (Stock stock : allStocks) {
            String mappingCode = null;
            String mediumCode = stock.getSectorMediumCode();
            String largeCode = stock.getSectorLargeCode();

            if (mediumCode != null && !mediumCode.equals("0000") && !mediumCode.isBlank()) {
                mappingCode = mediumCode.trim();
            } else if (largeCode != null && !largeCode.isBlank()) {
                mappingCode = largeCode.trim();
            }

            if (mappingCode != null) {
                sectorToStocksMap.computeIfAbsent(mappingCode, k -> new ArrayList<>()).add(stock);
            }
        }

        log.info(">>> Sector Mapping Check: Found {} stocks in {} mapped groups", allStocks.size(), sectorToStocksMap.size());

        // 2. [Pre-fetching] 모든 섹터의 전일 데이터 및 과거 시계열 로드
        Map<String, SectorInsight> yesterdayMap = sectorInsightPort.findLatestBeforeByCodes(sectorCodes, targetDate);
        Map<String, List<BigDecimal>> pastPricesMap = sectorInsightPort.findPastPricesByCodes(sectorCodes, targetDate, 119);

        List<SectorInsight> results = new ArrayList<>();
        for (MarketIndex index : indices) {
            try {
                String code = index.getIndexCode().trim();
                List<Stock> sectorStocks = sectorToStocksMap.get(code);

                if (sectorStocks == null || sectorStocks.isEmpty()) {
                    log.debug(">>> Sector {}({}): No matching stocks found in DB grouping", index.getIndexName(), code);
                    continue;
                }

                SectorInsight insight = analyzeSingleSectorFromDb(
                        index, targetDate, priceMap,
                        sectorStocks,
                        yesterdayMap.get(code),
                        pastPricesMap.getOrDefault(code, Collections.emptyList())
                );

                if (insight != null) {
                    results.add(insight);
                }
            } catch (Exception e) {
                log.error("섹터 {} 분석 중 오류 발생: {}", index.getIndexCode(), e.getMessage());
            }
        }
        return results;
    }

    /**
     * 외부 API 호출 없이 DB 데이터(종목 시세)만으로 섹터 인사이트를 분석
     */
    private SectorInsight analyzeSingleSectorFromDb(MarketIndex index, LocalDate date, Map<String, StockPrice> priceMap,
                                                    List<Stock> stocks, SectorInsight yesterdayData, List<BigDecimal> pastPrices) {
        // 해당 섹터에 속한 종목들의 시세를 priceMap에서 추출
        List<StockPrice> sectorPrices = stocks.stream()
                .map(s -> priceMap.get(s.getTicker()))
                .filter(Objects::nonNull)
                .toList();

        if (sectorPrices.isEmpty()) {
            return null;
        }

        // 지표 산출
        BigDecimal avgFluctuation = calculateAvgFluctuation(sectorPrices);
        
        // 지수 가격: 현재 DB에 섹터지수 시세 테이블이 별도로 없으므로, 
        // 전일 지수 가격에 평균 등락률을 곱해 추정하거나 0으로 세팅 (여기선 추정 방식 사용)
        BigDecimal estimatedSectorPrice = BigDecimal.ZERO;
        if (yesterdayData != null && yesterdayData.getSectorIndexCurrentPrice() != null) {
            BigDecimal multiplier = BigDecimal.ONE.add(avgFluctuation.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP));
            estimatedSectorPrice = yesterdayData.getSectorIndexCurrentPrice().multiply(multiplier).setScale(2, java.math.RoundingMode.HALF_UP);
        }

        // 수급 데이터: 현재 StockPrice에 투자자별 필드가 없으므로 0으로 세팅 (추후 확장 가능)
        Long netForeign = 0L;
        Long netInst = 0L;

        // 도메인 서비스로 분석 위임
        SectorApiDto apiDto = new SectorApiDto(
                index.getIndexCode(), index.getIndexName(), date, estimatedSectorPrice,
                avgFluctuation, netForeign, netInst
        );

        return sectorAnalysisService.analyze(index, apiDto, yesterdayData, pastPrices, sectorPrices);
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
