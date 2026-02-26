package org.stockwellness.batch.job.stock.price;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class StockPriceProcessor implements ItemProcessor<List<Stock>, List<StockPrice>> {

    private final KisDailyPriceAdapter kisAdapter;
    private final StockPricePort stockPricePort;

    @Value("#{jobParameters['startDate']}")
    private String startDateStr;

    @Value("#{jobParameters['endDate']}")
    private String endDateStr;

    private static final int CHUNK_DAYS = 100;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int INDICATOR_BUFFER_DAYS = 120;

    @Override
    public List<StockPrice> process(List<Stock> stocks) throws Exception {
        if (stocks == null || stocks.isEmpty()) return null;
        long startTime = System.currentTimeMillis();

        LocalDate endDate = StringUtils.hasText(endDateStr)
                ? LocalDate.parse(endDateStr, DATE_FMT)
                : LocalDate.now();

        Map<Long, LocalDate> latestDatesMap = stockPricePort.findLatestBaseDatesByStocks(stocks);

        List<Stock> todaySyncStocks = new ArrayList<>();
        List<Stock> gapSyncStocks = new ArrayList<>();

        for (Stock stock : stocks) {
            LocalDate lastDate = latestDatesMap.get(stock.getId());
            if (lastDate != null && ChronoUnit.DAYS.between(lastDate, endDate) == 1) {
                todaySyncStocks.add(stock);
            } else {
                gapSyncStocks.add(stock);
            }
        }

        List<StockPrice> resultEntities = new ArrayList<>();

        if (!todaySyncStocks.isEmpty()) {
            resultEntities.addAll(processMultiStockPrices(todaySyncStocks, endDate));
        }

        for (Stock stock : gapSyncStocks) {
            resultEntities.addAll(processIndividualGap(stock, latestDatesMap.get(stock.getId()), endDate));
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Batch Processed ({}ms): [Total: {}] TodaySync: {}, GapSync: {}, Created Entities: {}", 
                duration, stocks.size(), todaySyncStocks.size(), gapSyncStocks.size(), resultEntities.size());

        return resultEntities.isEmpty() ? null : resultEntities;
    }

    private List<StockPrice> processMultiStockPrices(List<Stock> stocks, LocalDate today) {
        List<String> tickers = stocks.stream().map(Stock::getTicker).toList();
        
        // [성능 최적화] 30개 종목 시세를 API 1회로 가져옴
        List<KisMultiStockPriceDetail> apiResults = kisAdapter.fetchMultiStockPrices(tickers);
        if (apiResults.isEmpty()) return Collections.emptyList();

        Map<String, KisMultiStockPriceDetail> apiResultMap = apiResults.stream()
                .collect(Collectors.toMap(KisMultiStockPriceDetail::ticker, d -> d));

        // [N+1 해결 2] 모든 종목의 과거 시세를 한 번에 조회
        Map<Long, List<BigDecimal>> historicalPricesMap = stockPricePort.findRecentClosingPricesByStocks(stocks, today, INDICATOR_BUFFER_DAYS);

        List<StockPrice> entities = new ArrayList<>();
        for (Stock stock : stocks) {
            KisMultiStockPriceDetail todayPrice = apiResultMap.get(stock.getTicker());
            if (todayPrice == null) continue;

            // [정합성 검증] 0원 이하 혹은 데이터 오류 방어
            BigDecimal currentPrice = new BigDecimal(todayPrice.closePrice());
            if (currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Stock {} has invalid price: {}. Skipping.", stock.getTicker(), currentPrice);
                continue;
            }

            List<BigDecimal> prices = new ArrayList<>(historicalPricesMap.getOrDefault(stock.getId(), Collections.emptyList()));
            prices.add(currentPrice);

            TechnicalIndicators indicators = TechnicalIndicatorCalculator.calculateLatest(prices);

            entities.add(StockPrice.of(
                    stock, today,
                    new BigDecimal(todayPrice.openPrice()),
                    new BigDecimal(todayPrice.highPrice()),
                    new BigDecimal(todayPrice.lowPrice()),
                    currentPrice,
                    currentPrice,
                    Long.parseLong(todayPrice.accumulatedVolume()),
                    new BigDecimal(todayPrice.accumulatedTradingValue()),
                    indicators
            ));
        }
        return entities;
    }

    private List<StockPrice> processIndividualGap(Stock stock, LocalDate latestBaseDate, LocalDate endDate) throws InterruptedException {
        LocalDate requestStartDate = StringUtils.hasText(startDateStr) ? LocalDate.parse(startDateStr, DATE_FMT) : endDate;
        LocalDate fetchStartDate = (latestBaseDate != null) ? latestBaseDate.plusDays(1) : requestStartDate.minusDays(250);

        if (fetchStartDate.isAfter(endDate)) return Collections.emptyList();

        List<KisDailyPriceDetail> apiResults = fetchPricesFromKis(stock, fetchStartDate, endDate);
        if (apiResults.isEmpty()) return Collections.emptyList();

        List<BigDecimal> historicalClosingPrices = stockPricePort.findRecentClosingPrices(stock, apiResults.get(0).baseDate(), INDICATOR_BUFFER_DAYS);
        List<BigDecimal> fullClosingPrices = new ArrayList<>(historicalClosingPrices);
        apiResults.sort(Comparator.comparing(KisDailyPriceDetail::baseDate));
        fullClosingPrices.addAll(apiResults.stream().map(KisDailyPriceDetail::closePrice).toList());

        List<TechnicalIndicators> allIndicators = TechnicalIndicatorCalculator.calculateSeries(fullClosingPrices);
        
        List<StockPrice> entities = new ArrayList<>();
        int indicatorStartIndex = historicalClosingPrices.size();

        for (int i = 0; i < apiResults.size(); i++) {
            KisDailyPriceDetail dto = apiResults.get(i);
            entities.add(StockPrice.of(
                    stock, dto.baseDate(), dto.openPrice(), dto.highPrice(), dto.lowPrice(),
                    dto.closePrice(), dto.closePrice(), dto.volume(), dto.transactionAmt(),
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
            List<KisDailyPriceDetail> response = kisAdapter.fetchDailyPrices(stock, chunkStartDate, cursorDate);
            Thread.sleep(70);
            if (response == null || response.isEmpty()) break;
            allDetails.addAll(response);
            LocalDate oldestDateInResponse = response.get(response.size() - 1).baseDate();
            if (oldestDateInResponse.isBefore(cursorDate)) cursorDate = oldestDateInResponse.minusDays(1);
            else break;
        }
        return allDetails;
    }
}
