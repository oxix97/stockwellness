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
public class SectorInsightItemProcessor implements ItemProcessor<SectorApiDto, SectorInsight> {

    private final MarketIndexPort marketIndexPort;
    private final SectorInsightPort sectorInsightPort;
    private final StockPort stockPort;
    private final StockPricePort stockPricePort;
    private final SectorAnalysisService sectorAnalysisService;

    // Cache for pre-fetched data to be reused across items in a single step execution
    private Map<String, StockPrice> priceMapCache;
    private Map<String, List<Stock>> sectorToStocksMapCache;
    private Map<String, MarketIndex> indexMapCache;

    @Override
    public SectorInsight process(SectorApiDto apiDto) {
        LocalDate targetDate = apiDto.baseDate();
        
        // Lazy initialize caches
        initializeCaches(targetDate);

        String code = apiDto.sectorCode();
        MarketIndex index = indexMapCache.get(code);
        if (index == null) {
            log.warn("MarketIndex not found for code: {}", code);
            return null;
        }

        List<Stock> sectorStocks = sectorToStocksMapCache.get(code);
        if (sectorStocks == null || sectorStocks.isEmpty()) {
            log.debug(">>> Sector {}({}): No matching stocks found in DB grouping", index.getIndexName(), code);
            return null;
        }

        // 해당 섹터에 속한 종목들의 시세를 priceMap에서 추출
        List<StockPrice> sectorStockPrices = sectorStocks.stream()
                .map(s -> priceMapCache.get(s.getTicker()))
                .filter(Objects::nonNull)
                .toList();

        // 전일 데이터 및 과거 시계열 로드
        SectorInsight yesterdayData = sectorInsightPort.findBySectorCodeAndDate(code, targetDate.minusDays(1))
                .orElse(sectorInsightPort.findLatestBeforeByCodes(List.of(code), targetDate).get(code));
        
        List<BigDecimal> pastPrices = sectorInsightPort.findPastPricesByCodes(List.of(code), targetDate, 119)
                .getOrDefault(code, Collections.emptyList());

        return sectorAnalysisService.analyze(index, apiDto, yesterdayData, pastPrices, sectorStockPrices);
    }

    private synchronized void initializeCaches(LocalDate targetDate) {
        if (priceMapCache != null) return;

        log.info("Initializing caches for SectorInsightItemProcessor (date: {})", targetDate);
        
        // 1. 모든 종목 시세 로드
        List<StockPrice> allPrices = stockPricePort.findAllByDate(targetDate);
        priceMapCache = allPrices.stream()
                .collect(Collectors.toMap(p -> p.getStock().getTicker(), p -> p, (p1, p2) -> p1));

        // 2. 모든 종목 로드 및 섹터 코드별 매핑
        List<Stock> allStocks = stockPort.findBySectorMediumCode(null);
        sectorToStocksMapCache = new HashMap<>();
        for (Stock stock : allStocks) {
            String mappingCode = getMappingCode(stock);
            if (mappingCode != null) {
                sectorToStocksMapCache.computeIfAbsent(mappingCode, k -> new ArrayList<>()).add(stock);
            }
        }

        // 3. 마켓 인덱스 맵
        indexMapCache = marketIndexPort.findAll().stream()
                .collect(Collectors.toMap(MarketIndex::getIndexCode, i -> i));
        
        log.info("Caches initialized: {} prices, {} mapped sectors, {} indices", 
                priceMapCache.size(), sectorToStocksMapCache.size(), indexMapCache.size());
    }

    private String getMappingCode(Stock stock) {
        String mediumCode = stock.getSector().getMediumCode();
        String largeCode = stock.getSector().getLargeCode();

        if (mediumCode != null && !mediumCode.equals("0000") && !mediumCode.isBlank()) {
            return mediumCode.trim();
        } else if (largeCode != null && !largeCode.isBlank()) {
            return largeCode.trim();
        }
        return null;
    }
}
