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

        // [N+1 방지] 청크 내 모든 종목의 마지막 저장일 및 과거 시계열 데이터(지표 계산용)를 미리 로드
        Map<Long, LocalDate> latestDatesMap = stockPricePort.findLatestBaseDatesByStocks(stocks);
        Map<Long, List<BigDecimal>> historicalPricesMap = stockPricePort.findRecentClosingPricesByStocks(stocks, endDate, INDICATOR_BUFFER_DAYS);

        List<Stock> todaySyncStocks = new ArrayList<>();
        List<Stock> gapSyncStocks = new ArrayList<>();

        for (Stock stock : stocks) {
            LocalDate lastDate = latestDatesMap.get(stock.getId());
            if (lastDate != null && DateUtil.daysBetween(lastDate, endDate) == 1) {
                todaySyncStocks.add(stock);
            } else {
                gapSyncStocks.add(stock);
            }
        }

        List<StockPrice> resultEntities = new ArrayList<>();

        if (!todaySyncStocks.isEmpty()) {
            resultEntities.addAll(processMultiStockPrices(todaySyncStocks, endDate, historicalPricesMap));
        }

        for (Stock stock : gapSyncStocks) {
            resultEntities.addAll(processIndividualGap(stock, latestDatesMap.get(stock.getId()), endDate, historicalPricesMap.getOrDefault(stock.getId(), Collections.emptyList())));
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Batch Processed ({}ms): [Total: {}] TodaySync: {}, GapSync: {}, Created Entities: {}", 
                duration, stocks.size(), todaySyncStocks.size(), gapSyncStocks.size(), resultEntities.size());

        return resultEntities.isEmpty() ? null : resultEntities;
    }

    private List<StockPrice> processMultiStockPrices(List<Stock> stocks, LocalDate today, Map<Long, List<BigDecimal>> historicalPricesMap) {
        List<String> tickers = stocks.stream().map(Stock::getTicker).toList();
        
        List<KisMultiStockPriceDetail> apiResults = kisAdapter.fetchMultiStockPrices(tickers);
        if (apiResults.isEmpty()) return Collections.emptyList();

        Map<String, KisMultiStockPriceDetail> apiResultMap = apiResults.stream()
                .collect(Collectors.toMap(KisMultiStockPriceDetail::ticker, d -> d));

        List<StockPrice> entities = new ArrayList<>();
        for (Stock stock : stocks) {
            KisMultiStockPriceDetail todayPrice = apiResultMap.get(stock.getTicker());
            if (todayPrice == null) continue;

            BigDecimal currentPrice = new BigDecimal(todayPrice.closePrice());
            if (currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Stock {} has invalid price: {}. Skipping.", stock.getTicker(), currentPrice);
                continue;
            }

            List<BigDecimal> pastPrices = historicalPricesMap.getOrDefault(stock.getId(), Collections.emptyList());
            
            // [요구사항 반영] API 응답에 의존하지 않고 내부 DB 데이터만 활용
            BigDecimal prevClose = BigDecimal.ZERO;
            if (!pastPrices.isEmpty()) {
                prevClose = pastPrices.get(pastPrices.size() - 1);
            } else {
                // 전일 종가가 없는데 신규 상장 종목이 아니면 예외 발생
                if (isNotNewListing(stock, today)) {
                    throw new IllegalStateException("전일 종가 데이터 누락 (신규 상장 아님): " + stock.getTicker());
                }
            }

            List<BigDecimal> pricesForIndicators = new ArrayList<>(pastPrices);
            pricesForIndicators.add(currentPrice);

            TechnicalIndicators indicators = TechnicalIndicatorCalculator.calculateLatest(pricesForIndicators);

            entities.add(StockPrice.of(
                    stock, today,
                    new BigDecimal(todayPrice.openPrice()),
                    new BigDecimal(todayPrice.highPrice()),
                    new BigDecimal(todayPrice.lowPrice()),
                    currentPrice,
                    currentPrice,
                    prevClose,
                    Long.parseLong(todayPrice.accumulatedVolume()),
                    new BigDecimal(todayPrice.accumulatedTradingValue()),
                    indicators
            ));
        }
        return entities;
    }

    private List<StockPrice> processIndividualGap(Stock stock, LocalDate latestBaseDate, LocalDate endDate, List<BigDecimal> preFetchedPrices) throws InterruptedException {
        LocalDate requestStartDate = StringUtils.hasText(startDateStr) ? DateUtil.parse(startDateStr) : endDate;
        LocalDate fetchStartDate = (latestBaseDate != null) ? latestBaseDate.plusDays(1) : requestStartDate.minusDays(250);

        if (fetchStartDate.isAfter(endDate)) return Collections.emptyList();

        List<KisDailyPriceDetail> apiResults = fetchPricesFromKis(stock, fetchStartDate, endDate);
        if (apiResults.isEmpty()) return Collections.emptyList();

        List<BigDecimal> historicalClosingPrices = preFetchedPrices;
        if (!apiResults.isEmpty() && latestBaseDate != null && DateUtil.daysBetween(latestBaseDate, apiResults.get(0).baseDate()) > 1) {
             historicalClosingPrices = stockPricePort.findRecentClosingPrices(stock, apiResults.get(0).baseDate(), INDICATOR_BUFFER_DAYS);
        }

        List<BigDecimal> fullClosingPrices = new ArrayList<>(historicalClosingPrices);
        apiResults.sort(Comparator.comparing(KisDailyPriceDetail::baseDate));
        fullClosingPrices.addAll(apiResults.stream().map(KisDailyPriceDetail::closePrice).toList());

        List<TechnicalIndicators> allIndicators = TechnicalIndicatorCalculator.calculateSeries(fullClosingPrices);
        
        List<StockPrice> entities = new ArrayList<>();
        int indicatorStartIndex = historicalClosingPrices.size();

        for (int i = 0; i < apiResults.size(); i++) {
            KisDailyPriceDetail dto = apiResults.get(i);
            
            // [요구사항 반영] 리스트 내 이전 데이터 혹은 DB 데이터 활용
            BigDecimal prevClose = BigDecimal.ZERO;
            if (i > 0) {
                prevClose = apiResults.get(i - 1).closePrice();
            } else if (!historicalClosingPrices.isEmpty()) {
                prevClose = historicalClosingPrices.get(historicalClosingPrices.size() - 1);
            } else {
                // 신규 상장 체크
                if (isNotNewListing(stock, dto.baseDate())) {
                    throw new IllegalStateException("전일 종가 데이터 누락 (신규 상장 아님): " + stock.getTicker() + " on " + dto.baseDate());
                }
            }

            entities.add(StockPrice.of(
                    stock, dto.baseDate(), dto.openPrice(), dto.highPrice(), dto.lowPrice(),
                    dto.closePrice(), dto.closePrice(), prevClose, dto.volume(), dto.transactionAmt(),
                    allIndicators.get(indicatorStartIndex + i)
            ));
        }
        return entities;
    }

    private boolean isNotNewListing(Stock stock, LocalDate baseDate) {
        if (stock.getListingDate() == null) return true; // 상장일 정보 없으면 보수적으로 신규 상장 아님으로 판단
        // 상장일이 분석 기준일(baseDate)과 같으면 신규 상장임. 상장일 이전이면 (데이터 오류지만) 신규 상장으로 간주 가능.
        // 따라서 상장일보다 기준일이 이후면 신규 상장이 아님.
        return baseDate.isAfter(stock.getListingDate());
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
