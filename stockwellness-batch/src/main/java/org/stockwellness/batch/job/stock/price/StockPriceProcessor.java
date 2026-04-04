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
import org.stockwellness.adapter.out.external.kis.dto.KisInvestorPriceDetail;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.batch.exception.BatchException;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.analysis.TechnicalIndicatorCalculator;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import org.stockwellness.global.error.ErrorCode;
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
    private static final int RECENT_SUPPLY_DEMAND_DAYS = 30; // 최근 30일 수급 데이터 수집
    private static final LocalDate EARLIEST_BASE_DATE = LocalDate.of(2022, 1, 1);

    @Override
    public List<StockPrice> process(List<Stock> stocks) throws Exception {
        if (stocks == null || stocks.isEmpty()) return null;
        long startTime = System.currentTimeMillis();

        LocalDate endDate = StringUtils.hasText(endDateStr) ? DateUtil.parse(endDateStr) : DateUtil.today();
        LocalDate paramStartDate = StringUtils.hasText(startDateStr) ? DateUtil.parse(startDateStr) : null;
        LocalDate effectiveStartDate = (paramStartDate != null && paramStartDate.isBefore(EARLIEST_BASE_DATE))
                ? EARLIEST_BASE_DATE : paramStartDate;

        boolean isExplicitRange = (effectiveStartDate != null);
        Map<Long, LocalDate> latestDatesMap = stockPricePort.findLatestBaseDatesByStocks(stocks);

        LocalDate lookbackBaseDate = isExplicitRange ? effectiveStartDate : endDate;
        Map<Long, List<StockPrice>> historicalEntitiesMap = stockPricePort.findRecentPricesWithDateByStocks(stocks,
                lookbackBaseDate, INDICATOR_BUFFER_DAYS);

        List<Stock> todaySyncStocks = new ArrayList<>();
        List<Stock> gapSyncStocks = new ArrayList<>();

        for (Stock stock : stocks) {
            LocalDate lastDate = latestDatesMap.get(stock.getId());
            if (!isExplicitRange && lastDate != null && DateUtil.daysBetween(lastDate, endDate) == 1) {
                todaySyncStocks.add(stock);
            } else {
                gapSyncStocks.add(stock);
            }
        }

        List<StockPrice> resultEntities = new ArrayList<>();
        if (!todaySyncStocks.isEmpty()) {
            try {
                resultEntities.addAll(processMultiStockPrices(todaySyncStocks, endDate, historicalEntitiesMap));
            } catch (Exception e) {
                log.error("{}개 종목의 멀티 시세 처리 실패: {}", todaySyncStocks.size(), e.getMessage());
            }
        }

        for (Stock stock : gapSyncStocks) {
            try {
                resultEntities.addAll(processIndividualGap(stock, latestDatesMap.get(stock.getId()), effectiveStartDate, endDate, historicalEntitiesMap.getOrDefault(stock.getId(), Collections.emptyList())));
            } catch (Exception e) {
                log.error("종목 {}의 개별 갭 처리 실패: {}", stock.getTicker(), e.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("배치 처리 완료 ({}ms): [총: {}] 당일동기화: {}, 범위동기화: {}, 생성된 엔티티: {}",
                duration, stocks.size(), todaySyncStocks.size(), gapSyncStocks.size(), resultEntities.size());

        return resultEntities.isEmpty() ? null : resultEntities;
    }

    private List<StockPrice> processMultiStockPrices(List<Stock> stocks, LocalDate today, Map<Long, List<StockPrice>> historicalEntitiesMap) {
        if (today.isBefore(EARLIEST_BASE_DATE)) return Collections.emptyList();

        List<String> tickers = stocks.stream().map(Stock::getTicker).toList();
        List<KisMultiStockPriceDetail> apiResults;
        try {
            apiResults = kisRateLimiter.executeSupplier(() -> kisAdapter.fetchMultiStockPrices(tickers));
        } catch (Exception e) {
            log.error("멀티 종목 시세 API 호출 실패: {}", e.getMessage());
            return Collections.emptyList();
        }

        if (apiResults.isEmpty()) return Collections.emptyList();
        Map<String, KisMultiStockPriceDetail> apiResultMap = apiResults.stream()
                .collect(Collectors.toMap(KisMultiStockPriceDetail::ticker, d -> d));

        List<StockPrice> entities = new ArrayList<>();
        for (Stock stock : stocks) {
            try {
                KisMultiStockPriceDetail todayPrice = apiResultMap.get(stock.getTicker());
                if (todayPrice == null) continue;

                BigDecimal currentPrice = new BigDecimal(todayPrice.closePrice());
                if (currentPrice.compareTo(BigDecimal.ZERO) <= 0) continue;

                List<StockPrice> pastEntities = historicalEntitiesMap.getOrDefault(stock.getId(), Collections.emptyList());
                BigDecimal prevClose = pastEntities.isEmpty() ? BigDecimal.ZERO : pastEntities.get(pastEntities.size() - 1).getClosePrice();

                List<BigDecimal> highPrices = new ArrayList<>(pastEntities.stream().map(p -> p.getHighPrice() != null ? p.getHighPrice() : p.getClosePrice()).toList());
                List<BigDecimal> lowPrices = new ArrayList<>(pastEntities.stream().map(p -> p.getLowPrice() != null ? p.getLowPrice() : p.getClosePrice()).toList());
                List<BigDecimal> closePrices = new ArrayList<>(pastEntities.stream().map(p -> p.getClosePrice() != null ? p.getClosePrice() : BigDecimal.ZERO).toList());
                List<LocalDate> dates = new ArrayList<>(pastEntities.stream().map(p -> p.getId().getBaseDate()).toList());

                highPrices.add(new BigDecimal(todayPrice.highPrice()));
                lowPrices.add(new BigDecimal(todayPrice.lowPrice()));
                closePrices.add(currentPrice);
                dates.add(today);

                TechnicalIndicators indicators = TechnicalIndicatorCalculator.calculateSeries(highPrices, lowPrices, closePrices, dates).stream()
                        .reduce((first, second) -> second).orElse(TechnicalIndicators.empty());

                BigDecimal instBuying = parseBigDecimal(todayPrice.netInstitutionalBuyingAmt());
                BigDecimal frgnBuying = parseBigDecimal(todayPrice.netForeignBuyingAmt());

                entities.add(StockPrice.of(stock, today, new BigDecimal(todayPrice.openPrice()), new BigDecimal(todayPrice.highPrice()),
                        new BigDecimal(todayPrice.lowPrice()), currentPrice, currentPrice, prevClose,
                        Long.parseLong(todayPrice.accumulatedVolume()), new BigDecimal(todayPrice.accumulatedTradingValue()),
                        instBuying, frgnBuying, indicators));
            } catch (Exception e) {
                log.error("종목 {}의 멀티 시세 처리 실패: {}", stock.getTicker(), e.getMessage());
            }
        }
        return entities;
    }

    private List<StockPrice> processIndividualGap(Stock stock, LocalDate latestBaseDate, LocalDate effectiveStartDate, LocalDate endDate, List<StockPrice> historicalEntities) {
        LocalDate storeStartDate = (effectiveStartDate != null) ? effectiveStartDate : 
                                    (latestBaseDate != null) ? latestBaseDate.plusDays(1) : EARLIEST_BASE_DATE;

        if (storeStartDate.isAfter(endDate)) return Collections.emptyList();

        LocalDate fetchStartDate = storeStartDate.minusDays(200);
        List<KisDailyPriceDetail> apiResults = fetchPricesFromKis(stock, fetchStartDate, endDate);
        if (apiResults.isEmpty()) return Collections.emptyList();

        // 수급 데이터 조회 (최근 30일분만)
        LocalDate investorFetchStart = endDate.minusDays(RECENT_SUPPLY_DEMAND_DAYS);
        Map<LocalDate, KisInvestorPriceDetail> investorMap = kisRateLimiter.executeSupplier(() -> 
                kisAdapter.fetchInvestorPrices(stock, investorFetchStart, endDate))
                .stream().collect(Collectors.toMap(KisInvestorPriceDetail::baseDate, d -> d, (v1, v2) -> v1));

        Map<LocalDate, OHLCRecord> mergedData = new TreeMap<>();
        for (StockPrice p : historicalEntities) {
            mergedData.put(p.getId().getBaseDate(), new OHLCRecord(
                    p.getOpenPrice(), p.getHighPrice(), p.getLowPrice(), p.getClosePrice(), p.getVolume(), p.getTransactionAmt(),
                    p.getNetInstitutionalBuyingAmt(), p.getNetForeignBuyingAmt()));
        }

        for (KisDailyPriceDetail dto : apiResults) {
            KisInvestorPriceDetail investor = investorMap.get(dto.baseDate());
            BigDecimal instBuying = (investor != null) ? investor.netInstitutionalBuyingAmt() : BigDecimal.ZERO;
            BigDecimal frgnBuying = (investor != null) ? investor.netForeignBuyingAmt() : BigDecimal.ZERO;

            mergedData.put(dto.baseDate(), new OHLCRecord(
                    dto.openPrice(), dto.highPrice(), dto.lowPrice(), dto.closePrice(), dto.volume(), dto.transactionAmt(),
                    instBuying, frgnBuying));
        }

        List<LocalDate> fullDates = new ArrayList<>(mergedData.keySet());
        List<BigDecimal> fullHighPrices = fullDates.stream().map(d -> mergedData.get(d).high()).toList();
        List<BigDecimal> fullLowPrices = fullDates.stream().map(d -> mergedData.get(d).low()).toList();
        List<BigDecimal> fullClosingPrices = fullDates.stream().map(d -> mergedData.get(d).close()).toList();

        List<TechnicalIndicators> allIndicators = TechnicalIndicatorCalculator.calculateSeries(fullHighPrices, fullLowPrices, fullClosingPrices, fullDates);

        List<StockPrice> entities = new ArrayList<>();
        for (int i = 0; i < fullDates.size(); i++) {
            LocalDate date = fullDates.get(i);
            if (date.isBefore(storeStartDate)) continue;

            BigDecimal prevClose = (i > 0) ? fullClosingPrices.get(i - 1) : BigDecimal.ZERO;
            OHLCRecord data = mergedData.get(date);

            entities.add(StockPrice.of(stock, date, data.open(), data.high(), data.low(), data.close(), data.close(), prevClose,
                    data.volume(), data.transactionAmt(), data.instBuying(), data.frgnBuying(), allIndicators.get(i)));
        }
        return entities;
    }

    private List<KisDailyPriceDetail> fetchPricesFromKis(Stock stock, LocalDate start, LocalDate end) {
        if (!stock.getTicker().matches("^[0-9]+$")) return Collections.emptyList();
        List<KisDailyPriceDetail> allDetails = new ArrayList<>();
        LocalDate cursorDate = end;
        while (!cursorDate.isBefore(start)) {
            LocalDate chunkStartDate = cursorDate.minusDays(CHUNK_DAYS);
            if (chunkStartDate.isBefore(start)) chunkStartDate = start;

            final LocalDate finalChunkStartDate = chunkStartDate;
            final LocalDate finalCursorDate = cursorDate;
            List<KisDailyPriceDetail> response = kisRateLimiter.executeSupplier(() ->
                    kisAdapter.fetchDailyPrices(stock, finalChunkStartDate, finalCursorDate));

            if (response.isEmpty()) {
                cursorDate = chunkStartDate.minusDays(1);
                continue;
            }

            allDetails.addAll(response);
            LocalDate oldestDateInResponse = response.get(response.size() - 1).baseDate();
            cursorDate = oldestDateInResponse.isBefore(cursorDate) ? oldestDateInResponse.minusDays(1) : chunkStartDate.minusDays(1);
        }
        return allDetails;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (!StringUtils.hasText(value)) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private record OHLCRecord(
            BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, Long volume, BigDecimal transactionAmt,
            BigDecimal instBuying, BigDecimal frgnBuying
    ) {}
}
