package org.stockwellness.batch.job.stock.price;

import io.github.resilience4j.ratelimiter.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail;
import org.stockwellness.adapter.out.external.kis.dto.KisMultiStockPriceDetail;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.analysis.TechnicalIndicatorCalculator;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import org.stockwellness.global.util.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class StockPriceProcessor implements ItemProcessor<List<Stock>, List<StockPrice>> {

    private final KisDailyPriceAdapter kisAdapter;
    private final StockPricePort stockPricePort;
    private final RateLimiter kisRateLimiter;

    @Value("#{jobParameters['startDate']}")
    private String startDateStr;

    @Value("#{jobParameters['endDate']}")
    private String endDateStr;

    private static final int CHUNK_DAYS = 100;
    private static final int INDICATOR_BUFFER_DAYS = 120;

    @Override
    public List<StockPrice> process(List<Stock> stocks) throws Exception {
        if (stocks == null || stocks.isEmpty()) return null;
        long startTime = System.currentTimeMillis();

        LocalDate endDate = StringUtils.hasText(endDateStr)
                ? DateUtil.parse(endDateStr)
                : DateUtil.today();

        // 파라미터가 명시적으로 있으면 '소급(Backfill) 모드'로 간주
        boolean isExplicitRange = StringUtils.hasText(startDateStr);

        Map<Long, LocalDate> latestDatesMap = stockPricePort.findLatestBaseDatesByStocks(stocks);
        // 지표 계산을 위한 과거 데이터는 공통적으로 로드 (최적화 - 엔티티로 로드하여 날짜 정보 포함)
        Map<Long, List<StockPrice>> historicalEntitiesMap = stockPricePort.findRecentPricesWithDateByStocks(stocks, 
                isExplicitRange ? DateUtil.parse(startDateStr) : endDate, INDICATOR_BUFFER_DAYS);

        List<Stock> todaySyncStocks = new ArrayList<>();
        List<Stock> gapSyncStocks = new ArrayList<>();

        for (Stock stock : stocks) {
            LocalDate lastDate = latestDatesMap.get(stock.getId());
            
            // 파라미터가 없고 어제까지 데이터가 있다면 '당일 단순 업데이트'
            if (!isExplicitRange && lastDate != null && DateUtil.daysBetween(lastDate, endDate) == 1) {
                todaySyncStocks.add(stock);
            } else {
                // 파라미터가 있거나 데이터 간격이 크면 '범위 업데이트'
                gapSyncStocks.add(stock);
            }
        }

        List<StockPrice> resultEntities = new ArrayList<>();

        if (!todaySyncStocks.isEmpty()) {
            resultEntities.addAll(processMultiStockPrices(todaySyncStocks, endDate, historicalEntitiesMap));
        }

        for (Stock stock : gapSyncStocks) {
            resultEntities.addAll(processIndividualGap(stock, latestDatesMap.get(stock.getId()), endDate, historicalEntitiesMap.getOrDefault(stock.getId(), Collections.emptyList())));
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Batch Processed ({}ms): [Total: {}] TodaySync: {}, RangeSync: {}, Created Entities: {}", 
                duration, stocks.size(), todaySyncStocks.size(), gapSyncStocks.size(), resultEntities.size());

        return resultEntities.isEmpty() ? null : resultEntities;
    }

    private List<StockPrice> processMultiStockPrices(List<Stock> stocks, LocalDate today, Map<Long, List<StockPrice>> historicalEntitiesMap) {
        List<String> tickers = stocks.stream().map(Stock::getTicker).toList();
        
        // RateLimiter 적용
        List<KisMultiStockPriceDetail> apiResults = kisRateLimiter.executeSupplier(() -> 
                kisAdapter.fetchMultiStockPrices(tickers));
        
        if (apiResults.isEmpty()) return Collections.emptyList();

        Map<String, KisMultiStockPriceDetail> apiResultMap = apiResults.stream()
                .collect(Collectors.toMap(KisMultiStockPriceDetail::ticker, d -> d));

        List<StockPrice> entities = new ArrayList<>();
        for (Stock stock : stocks) {
            KisMultiStockPriceDetail todayPrice = apiResultMap.get(stock.getTicker());
            if (todayPrice == null) continue;

            BigDecimal currentPrice = new BigDecimal(todayPrice.closePrice());
            if (currentPrice.compareTo(BigDecimal.ZERO) <= 0) continue;

            List<StockPrice> pastEntities = historicalEntitiesMap.getOrDefault(stock.getId(), Collections.emptyList());
            BigDecimal lastPastClose = pastEntities.isEmpty() ? BigDecimal.ZERO : pastEntities.get(pastEntities.size() - 1).getClosePrice();
            BigDecimal prevClose = (lastPastClose != null) ? lastPastClose : BigDecimal.ZERO;

            List<BigDecimal> highPrices = new ArrayList<>(pastEntities.stream().map(p -> p.getHighPrice() != null ? p.getHighPrice() : p.getClosePrice()).toList());
            List<BigDecimal> lowPrices = new ArrayList<>(pastEntities.stream().map(p -> p.getLowPrice() != null ? p.getLowPrice() : p.getClosePrice()).toList());
            List<BigDecimal> closePrices = new ArrayList<>(pastEntities.stream().map(p -> p.getClosePrice() != null ? p.getClosePrice() : BigDecimal.ZERO).toList());

            highPrices.add(new BigDecimal(todayPrice.highPrice()));
            lowPrices.add(new BigDecimal(todayPrice.lowPrice()));
            closePrices.add(currentPrice);

            // OHLC 데이터를 사용하여 최신 지표 계산
            List<TechnicalIndicators> indicatorSeries = TechnicalIndicatorCalculator.calculateSeries(highPrices, lowPrices, closePrices, null);
            TechnicalIndicators indicators = indicatorSeries.isEmpty() ? TechnicalIndicators.empty() : indicatorSeries.get(indicatorSeries.size() - 1);

            entities.add(StockPrice.of(
                    stock, today,
                    new BigDecimal(todayPrice.openPrice()),
                    new BigDecimal(todayPrice.highPrice()),
                    new BigDecimal(todayPrice.lowPrice()),
                    currentPrice, currentPrice, prevClose,
                    Long.parseLong(todayPrice.accumulatedVolume()),
                    new BigDecimal(todayPrice.accumulatedTradingValue()),
                    indicators
            ));
        }
        return entities;
    }

    private List<StockPrice> processIndividualGap(Stock stock, LocalDate latestBaseDate, LocalDate endDate, List<StockPrice> historicalEntities) throws InterruptedException {
        // [수정] 시작일 결정 전략
        LocalDate fetchStartDate;
        if (StringUtils.hasText(startDateStr)) {
            // 1. 파라미터가 명시되면 해당 날짜부터 (Backfill 모드)
            fetchStartDate = DateUtil.parse(startDateStr);
        } else {
            // 2. 파라미터가 없으면 마지막 저장일 다음날부터 (증분 업데이트 모드)
            fetchStartDate = (latestBaseDate != null) ? latestBaseDate.plusDays(1) : endDate.minusDays(250);
        }

        if (fetchStartDate.isAfter(endDate)) return Collections.emptyList();

        List<KisDailyPriceDetail> apiResults = fetchPricesFromKis(stock, fetchStartDate, endDate);
        if (apiResults.isEmpty()) return Collections.emptyList();

        // 지표 계산용 버퍼 로드 (실제 날짜 사용)
        List<BigDecimal> historicalHighPrices = new ArrayList<>(historicalEntities.stream().map(p -> p.getHighPrice() != null ? p.getHighPrice() : p.getClosePrice()).toList());
        List<BigDecimal> historicalLowPrices = new ArrayList<>(historicalEntities.stream().map(p -> p.getLowPrice() != null ? p.getLowPrice() : p.getClosePrice()).toList());
        List<BigDecimal> historicalClosingPrices = new ArrayList<>(historicalEntities.stream().map(StockPrice::getClosePrice).toList());
        List<LocalDate> historicalDates = new ArrayList<>(historicalEntities.stream().map(p -> p.getId().getBaseDate()).toList());
        
        // 만약 historicalEntities가 비어있는데 DB에 데이터가 있다면 추가 조회
        if (historicalEntities.isEmpty() && latestBaseDate != null) {
            historicalClosingPrices = stockPricePort.findRecentClosingPrices(stock, fetchStartDate, INDICATOR_BUFFER_DAYS);
            historicalHighPrices = new ArrayList<>(historicalClosingPrices);
            historicalLowPrices = new ArrayList<>(historicalClosingPrices);
        }

        apiResults.sort(Comparator.comparing(KisDailyPriceDetail::baseDate));

        List<BigDecimal> fullHighPrices = new ArrayList<>(historicalHighPrices);
        fullHighPrices.addAll(apiResults.stream().map(KisDailyPriceDetail::highPrice).toList());

        List<BigDecimal> fullLowPrices = new ArrayList<>(historicalLowPrices);
        fullLowPrices.addAll(apiResults.stream().map(KisDailyPriceDetail::lowPrice).toList());

        List<BigDecimal> fullClosingPrices = new ArrayList<>(historicalClosingPrices);
        fullClosingPrices.addAll(apiResults.stream().map(KisDailyPriceDetail::closePrice).toList());

        List<LocalDate> fullDates = new ArrayList<>(historicalDates);
        fullDates.addAll(apiResults.stream().map(KisDailyPriceDetail::baseDate).toList());

        List<TechnicalIndicators> allIndicators = TechnicalIndicatorCalculator.calculateSeries(fullHighPrices, fullLowPrices, fullClosingPrices, fullDates);
        
        List<StockPrice> entities = new ArrayList<>();
        int indicatorStartIndex = historicalClosingPrices.size();

        for (int i = 0; i < apiResults.size(); i++) {
            KisDailyPriceDetail dto = apiResults.get(i);
            BigDecimal prevClosePriceInLoop = (i > 0) ? apiResults.get(i - 1).closePrice() : 
                                  (historicalClosingPrices.isEmpty() ? BigDecimal.ZERO : historicalClosingPrices.get(historicalClosingPrices.size() - 1));
            
            BigDecimal prevClose = (prevClosePriceInLoop != null) ? prevClosePriceInLoop : BigDecimal.ZERO;

            entities.add(StockPrice.of(
                    stock, dto.baseDate(), dto.openPrice(), dto.highPrice(), dto.lowPrice(),
                    dto.closePrice(), dto.closePrice(), prevClose, dto.volume(), dto.transactionAmt(),
                    allIndicators.get(indicatorStartIndex + i)
            ));
        }
        return entities;
    }

    private List<KisDailyPriceDetail> fetchPricesFromKis(Stock stock, LocalDate start, LocalDate end) throws InterruptedException {
        List<KisDailyPriceDetail> allDetails = new ArrayList<>();
        LocalDate cursorDate = end;
        while (!cursorDate.isBefore(start)) {
            LocalDate chunkStartDate = cursorDate.minusDays(CHUNK_DAYS);
            if (chunkStartDate.isBefore(start)) chunkStartDate = start;

            // RateLimiter 적용
            final LocalDate finalChunkStartDate = chunkStartDate;
            final LocalDate finalCursorDate = cursorDate;
            List<KisDailyPriceDetail> response = kisRateLimiter.executeSupplier(() -> 
                    kisAdapter.fetchDailyPrices(stock, finalChunkStartDate, finalCursorDate));
            
            if (response == null || response.isEmpty()) break;
            allDetails.addAll(response);
            LocalDate oldestDateInResponse = response.get(response.size() - 1).baseDate();
            if (oldestDateInResponse.isBefore(cursorDate)) cursorDate = oldestDateInResponse.minusDays(1);
            else break;
        }
        return allDetails;
    }
}
