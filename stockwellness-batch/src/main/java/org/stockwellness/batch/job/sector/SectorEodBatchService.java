package org.stockwellness.batch.job.sector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.stockwellness.application.port.in.batch.SectorEodSyncUseCase;
import org.stockwellness.application.port.out.sector.LoadSectorAiPort;
import org.stockwellness.application.port.out.sector.SectorAiContext;
import org.stockwellness.application.port.out.stock.MarketIndexPort;
import org.stockwellness.application.port.out.stock.SectorInsightPort;
import org.stockwellness.application.port.out.stock.StockPort;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.application.service.stock.SectorAnalysisService;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.analysis.AiReport;
import org.stockwellness.domain.stock.analysis.TrendStatus;
import org.stockwellness.domain.stock.insight.MarketIndex;
import org.stockwellness.domain.stock.insight.SectorAiOpinion;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorEodBatchService implements SectorEodSyncUseCase {

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

    @Override
    public SectorEodResult syncSector(SectorSyncCommand command) {
        var apiDto = command.sectorApiDto();
        initializeCaches(apiDto.baseDate());

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

        SectorInsight yesterdayData = sectorInsightPort.findBySectorCodeAndDate(apiDto.sectorCode(), apiDto.baseDate().minusDays(1))
                .orElse(sectorInsightPort.findLatestBeforeByCodes(List.of(apiDto.sectorCode()), apiDto.baseDate()).get(apiDto.sectorCode()));
        List<BigDecimal> pastPrices = sectorInsightPort.findPastPricesByCodes(List.of(apiDto.sectorCode()), apiDto.baseDate(), 119)
                .getOrDefault(apiDto.sectorCode(), Collections.emptyList());

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

        try {
            SectorAiContext context = new SectorAiContext(
                    insight.getSectorName(),
                    insight.getSectorCode(),
                    insight.getMarketType(),
                    insight.getBaseDate(),
                    insight.getIndicators().getSectorIndexCurrentPrice(),
                    insight.getIndicators().getAvgFluctuationRate(),
                    insight.getIndicators().getNetForeignBuyAmount(),
                    insight.getIndicators().getNetInstBuyAmount(),
                    insight.getIndicators().getForeignConsecutiveBuyDays(),
                    insight.getIndicators().getInstConsecutiveBuyDays(),
                    resolveTrendStatus(insight.getTechnicalIndicators()),
                    insight.getTechnicalIndicators() != null ? insight.getTechnicalIndicators().getRsi14() : null,
                    insight.isOverheated(),
                    insight.getLeadingStocks()
            );

            AiReport report = loadSectorAiPort.generateSectorOpinion(context);
            insight.updateAiOpinion(SectorAiOpinion.of(
                    report.decision(),
                    report.confidenceScore(),
                    report.title(),
                    report.keyReasons(),
                    report.detailedAnalysis()
            ));
        } catch (Exception e) {
            AiReport fallback = AiReport.fallback();
            insight.updateAiOpinion(SectorAiOpinion.of(
                    fallback.decision(),
                    fallback.confidenceScore(),
                    fallback.title(),
                    fallback.keyReasons(),
                    fallback.detailedAnalysis()
            ));
        }

        return new SectorEodResult(insight);
    }

    private synchronized void initializeCaches(LocalDate targetDate) {
        if (targetDate.equals(cachedDate) && priceMapCache != null && sectorToStocksMapCache != null && indexMapCache != null) {
            return;
        }

        priceMapCache = stockPricePort.findAllByDate(targetDate).stream()
                .collect(Collectors.toMap(price -> price.getStock().getTicker(), price -> price, (left, right) -> left));

        List<Stock> allStocks = stockPort.findBySectorMediumCode(null);
        sectorToStocksMapCache = new HashMap<>();
        for (Stock stock : allStocks) {
            String mappingCode = getMappingCode(stock);
            if (mappingCode != null) {
                sectorToStocksMapCache.computeIfAbsent(mappingCode, key -> new ArrayList<>()).add(stock);
            }
        }

        indexMapCache = marketIndexPort.findAll().stream()
                .collect(Collectors.toMap(MarketIndex::getIndexCode, value -> value));
        cachedDate = targetDate;
    }

    private String getMappingCode(Stock stock) {
        String mediumCode = stock.getSector().getMediumCode();
        if (mediumCode != null && !mediumCode.equals("0000") && !mediumCode.isBlank()) {
            return mediumCode.trim();
        }
        String largeCode = stock.getSector().getLargeCode();
        if (largeCode != null && !largeCode.isBlank()) {
            return largeCode.trim();
        }
        return null;
    }

    private TrendStatus resolveTrendStatus(TechnicalIndicators indicators) {
        if (indicators == null || indicators.getMa5() == null || indicators.getMa20() == null || indicators.getMa60() == null) {
            return TrendStatus.NEUTRAL;
        }
        if (indicators.getMa5().compareTo(indicators.getMa20()) > 0 && indicators.getMa20().compareTo(indicators.getMa60()) > 0) {
            return TrendStatus.REGULAR;
        }
        if (indicators.getMa5().compareTo(indicators.getMa20()) < 0 && indicators.getMa20().compareTo(indicators.getMa60()) < 0) {
            return TrendStatus.INVERSE;
        }
        return TrendStatus.NEUTRAL;
    }
}
