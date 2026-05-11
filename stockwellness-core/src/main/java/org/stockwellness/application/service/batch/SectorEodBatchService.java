package org.stockwellness.application.service.batch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.stockwellness.application.port.in.batch.SectorEodSyncUseCase;
import org.stockwellness.application.port.out.sector.LoadSectorAiPort;
import org.stockwellness.application.port.out.stock.MarketIndexPort;
import org.stockwellness.application.port.out.stock.SectorInsightPort;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.application.service.stock.SectorAnalysisService;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.domain.stock.price.StockPrice;

@Service
@RequiredArgsConstructor
@Slf4j
public class SectorEodBatchService implements SectorEodSyncUseCase {

    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    private final MarketIndexPort marketIndexPort;
    private final SectorInsightPort sectorInsightPort;
    private final StockPort stockPort;
    private final StockPricePort stockPricePort;
    private final SectorAnalysisService sectorAnalysisService;
    private final LoadSectorAiPort loadSectorAiPort;

    private volatile LocalDate cachedDate;
    private Map<String, StockPrice> priceMapCache;
    private Map<String, List<Stock>> sectorToStocksMapCache;
    private Map<String, MarketIndex> indexMapCache;
    private Map<String, SectorInsight> previousInsightMapCache;
    private Map<String, List<BigDecimal>> pastPricesMapCache;
    private Set<String> preparedSectorCodes = Collections.emptySet();

    @Override
    public void prepareSync(LocalDate targetDate, List<String> sectorCodes) {
        List<String> normalizedCodes = normalizeSectorCodes(sectorCodes);
        if (normalizedCodes.isEmpty()) {
            initializeCaches(targetDate);
            return;
        }

        synchronized (this) {
            initializeCaches(targetDate);

            Set<String> missingCodes = normalizedCodes.stream()
                    .filter(code -> !preparedSectorCodes.contains(code))
                    .collect(Collectors.toCollection(HashSet::new));

            if (missingCodes.isEmpty() && previousInsightMapCache != null && pastPricesMapCache != null) {
                return;
            }

            if (previousInsightMapCache == null) {
                previousInsightMapCache = new HashMap<>();
            }
            if (pastPricesMapCache == null) {
                pastPricesMapCache = new HashMap<>();
            }

            Map<String, SectorInsight> previousInsights = sectorInsightPort.findLatestBeforeByCodes(List.copyOf(missingCodes), targetDate);
            Map<String, List<BigDecimal>> pastPrices = sectorInsightPort.findPastPricesByCodes(List.copyOf(missingCodes), targetDate, 119);

            previousInsightMapCache.putAll(previousInsights);
            for (String sectorCode : missingCodes) {
                pastPricesMapCache.put(sectorCode, pastPrices.getOrDefault(sectorCode, Collections.emptyList()));
            }

            Set<String> updatedPreparedSectorCodes = new HashSet<>(preparedSectorCodes);
            updatedPreparedSectorCodes.addAll(missingCodes);
            preparedSectorCodes = updatedPreparedSectorCodes;
        }
    }

    @Override
    public SectorEodResult syncSector(SectorSyncCommand command) {
        var apiDto = command.sectorApiDto();
        prepareSync(apiDto.baseDate(), List.of(apiDto.sectorCode()));

        MarketIndex index = indexMapCache.get(apiDto.sectorCode());
        if (index == null) {
            log.warn("해당 코드에 대한 MarketIndex를 찾을 수 없음: {}", apiDto.sectorCode());
            return new SectorEodResult(null);
        }

        List<Stock> sectorStocks = sectorToStocksMapCache.getOrDefault(apiDto.sectorCode(), Collections.emptyList());
        List<StockPrice> sectorStockPrices = sectorStocks.stream()
                .map(stock -> priceMapCache.get(stock.getTicker()))
                .filter(Objects::nonNull)
                .toList();

        SectorInsight yesterdayData = previousInsightMapCache.get(apiDto.sectorCode());
        List<BigDecimal> pastPrices = pastPricesMapCache.getOrDefault(apiDto.sectorCode(), Collections.emptyList());

        return new SectorEodResult(
                sectorAnalysisService.analyze(index, apiDto, yesterdayData, pastPrices, sectorStockPrices)
        );
    }

    @Override
    public SectorEodResult enrichAiOpinion(SectorAiAnalysisCommand command) {
        SectorInsight insight = command.sectorInsight();
        if (insight == null) {
            return new SectorEodResult(null);
        }

        if (!aiEnabled) {
            return new SectorEodResult(insight);
        }

        // AI 사용량 및 요금 절감을 위해 플래그로 제어 (기존 로직 주석 유지)
        /*
        try {
            SectorAiContext context = new SectorAiContext(
...
            insight.updateAiOpinion(SectorAiOpinion.of(
                    fallback.decision(),
                    fallback.confidenceScore(),
                    fallback.title(),
                    fallback.keyReasons(),
                    fallback.detailedAnalysis()
            ));
        }
        */

        return new SectorEodResult(insight);
    }

    private synchronized void initializeCaches(LocalDate targetDate) {
        if (targetDate.equals(cachedDate) && priceMapCache != null && sectorToStocksMapCache != null && indexMapCache != null) {
            return;
        }

        priceMapCache = stockPricePort.findAllByDate(targetDate).stream()
                .collect(Collectors.toMap(price -> price.getStock().getTicker(), price -> price, (left, right) -> left));

        List<Stock> allStocks = stockPort.findBySectorCode(null);
        sectorToStocksMapCache = new HashMap<>();
        for (Stock stock : allStocks) {
            String sectorCode = stock.getSector().getSectorCode();
            if (sectorCode != null) {
                sectorToStocksMapCache.computeIfAbsent(sectorCode, key -> new ArrayList<>()).add(stock);
            }
        }

        indexMapCache = marketIndexPort.findAll().stream()
                .collect(Collectors.toMap(MarketIndex::getIndexCode, value -> value));
        previousInsightMapCache = new HashMap<>();
        pastPricesMapCache = new HashMap<>();
        preparedSectorCodes = new HashSet<>();
        cachedDate = targetDate;
    }

    private List<String> normalizeSectorCodes(List<String> sectorCodes) {
        if (sectorCodes == null || sectorCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return sectorCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isBlank())
                .distinct()
                .toList();
    }
}
