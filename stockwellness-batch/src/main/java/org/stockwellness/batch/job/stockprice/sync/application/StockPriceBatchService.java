package org.stockwellness.batch.job.stockprice.sync.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.stockwellness.application.port.in.batch.StockPriceRepairUseCase;
import org.stockwellness.application.port.in.batch.StockPriceSyncUseCase;
import org.stockwellness.application.port.out.stock.DailyStockPriceSnapshot;
import org.stockwellness.application.port.out.stock.InvestorTradingSnapshot;
import org.stockwellness.application.port.out.stock.MultiStockPriceSnapshot;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.analysis.TechnicalIndicatorCalculator;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;
import org.stockwellness.global.util.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.stockwellness.adapter.out.external.kis.exception.KisApiException;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockPriceBatchService implements StockPriceSyncUseCase, StockPriceRepairUseCase {

    private static final int CHUNK_DAYS = 100;
    private static final int INDICATOR_BUFFER_DAYS = 120;
    private static final int RECENT_SUPPLY_DEMAND_DAYS = 30;
    private static final LocalDate EARLIEST_BASE_DATE = LocalDate.of(2022, 1, 1);

    private final StockPricePort stockPricePort;
    private final AtomicInteger kisRateLimitDetectionCount = new AtomicInteger();

    @Override
    public StockPriceSyncResult sync(StockPriceBatchCommand command) {
        List<Stock> stocks = command.stocks();
        if (stocks == null || stocks.isEmpty()) {
            return new StockPriceSyncResult(List.of());
        }

        LocalDate endDate = StringUtils.hasText(command.endDate()) ? DateUtil.parse(command.endDate()) : DateUtil.today();
        LocalDate paramStartDate = StringUtils.hasText(command.startDate()) ? DateUtil.parse(command.startDate()) : null;
        LocalDate effectiveStartDate = (paramStartDate != null && paramStartDate.isBefore(EARLIEST_BASE_DATE))
                ? EARLIEST_BASE_DATE
                : paramStartDate;

        boolean isExplicitRange = effectiveStartDate != null;
        Map<Long, LocalDate> latestDatesMap = stockPricePort.findLatestBaseDatesByStocks(stocks);
        LocalDate lookbackBaseDate = isExplicitRange ? effectiveStartDate : endDate;
        Map<Long, List<StockPrice>> historicalEntitiesMap = stockPricePort.findRecentPricesWithDateByStocks(
                stocks,
                lookbackBaseDate,
                INDICATOR_BUFFER_DAYS
        );

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
            resultEntities.addAll(processMultiStockPrices(todaySyncStocks, endDate, historicalEntitiesMap));
        }

        for (Stock stock : gapSyncStocks) {
            resultEntities.addAll(processIndividualGap(
                    stock,
                    latestDatesMap.get(stock.getId()),
                    effectiveStartDate,
                    endDate,
                    historicalEntitiesMap.getOrDefault(stock.getId(), Collections.emptyList())
            ));
        }

        return new StockPriceSyncResult(resultEntities);
    }

    @Override
    public StockPriceRepairResult repair(StockPriceRepairCommand command) {
        List<StockPrice> allPrices = stockPricePort.findRecentPricesWithDateByStocks(
                List.of(command.stock()),
                LocalDate.now(),
                Integer.MAX_VALUE
        ).getOrDefault(command.stock().getId(), List.of());
        if (allPrices.isEmpty()) {
            return new StockPriceRepairResult(List.of());
        }

        LocalDate reqStart = DateUtil.parse(command.startDate());
        LocalDate reqEnd = DateUtil.parse(command.endDate());
        List<StockPriceRepairRow> toUpdate = new ArrayList<>();
        BigDecimal previousClose = null;

        for (StockPrice current : allPrices) {
            LocalDate currentBaseDate = current.getId().getBaseDate();
            if (previousClose != null) {
                boolean inRange = DateUtil.isBetween(currentBaseDate, reqStart, reqEnd);
                boolean needsRepair = current.getPreviousClosePrice() == null
                        || current.getPreviousClosePrice().compareTo(BigDecimal.ZERO) == 0;
                if (inRange && needsRepair) {
                    toUpdate.add(new StockPriceRepairRow(
                            command.stock().getId(),
                            currentBaseDate,
                            previousClose
                    ));
                }
            }
            previousClose = current.getClosePrice();
        }

        return new StockPriceRepairResult(toUpdate);
    }

    private List<StockPrice> processMultiStockPrices(
            List<Stock> stocks,
            LocalDate today,
            Map<Long, List<StockPrice>> historicalEntitiesMap
    ) {
        if (today.isBefore(EARLIEST_BASE_DATE)) {
            return List.of();
        }

        List<MultiStockPriceSnapshot> apiResults = executeKisCall("multi-stock-prices", () ->
                stockPricePort.fetchMultiStockPrices(stocks.stream().map(Stock::getTicker).toList())
        );
        Map<String, MultiStockPriceSnapshot> apiResultMap = apiResults.stream()
                .collect(Collectors.toMap(MultiStockPriceSnapshot::ticker, value -> value));

        List<StockPrice> entities = new ArrayList<>();
        for (Stock stock : stocks) {
            MultiStockPriceSnapshot todayPrice = apiResultMap.get(stock.getTicker());
            if (todayPrice == null || todayPrice.closePrice() == null || todayPrice.closePrice().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            List<StockPrice> pastEntities = historicalEntitiesMap.getOrDefault(stock.getId(), Collections.emptyList());
            BigDecimal prevClose = pastEntities.isEmpty()
                    ? BigDecimal.ZERO
                    : pastEntities.get(pastEntities.size() - 1).getClosePrice();

            List<BigDecimal> highPrices = new ArrayList<>(pastEntities.stream()
                    .map(price -> price.getHighPrice() != null ? price.getHighPrice() : price.getClosePrice())
                    .toList());
            List<BigDecimal> lowPrices = new ArrayList<>(pastEntities.stream()
                    .map(price -> price.getLowPrice() != null ? price.getLowPrice() : price.getClosePrice())
                    .toList());
            List<BigDecimal> closePrices = new ArrayList<>(pastEntities.stream()
                    .map(price -> price.getClosePrice() != null ? price.getClosePrice() : BigDecimal.ZERO)
                    .toList());
            List<LocalDate> dates = new ArrayList<>(pastEntities.stream().map(price -> price.getId().getBaseDate()).toList());

            highPrices.add(todayPrice.highPrice());
            lowPrices.add(todayPrice.lowPrice());
            closePrices.add(todayPrice.closePrice());
            dates.add(today);

            TechnicalIndicators indicators = TechnicalIndicatorCalculator.calculateSeries(highPrices, lowPrices, closePrices, dates)
                    .stream()
                    .reduce((first, second) -> second)
                    .orElse(TechnicalIndicators.empty());

            entities.add(StockPrice.of(
                    stock,
                    today,
                    todayPrice.openPrice(),
                    todayPrice.highPrice(),
                    todayPrice.lowPrice(),
                    todayPrice.closePrice(),
                    todayPrice.closePrice(),
                    prevClose,
                    todayPrice.accumulatedVolume(),
                    todayPrice.accumulatedTradingValue(),
                    defaultDecimal(todayPrice.netInstitutionalBuyingAmt()),
                    defaultDecimal(todayPrice.netForeignBuyingAmt()),
                    indicators
            ));
        }

        return entities;
    }

    private List<StockPrice> processIndividualGap(
            Stock stock,
            LocalDate latestBaseDate,
            LocalDate effectiveStartDate,
            LocalDate endDate,
            List<StockPrice> historicalEntities
    ) {
        LocalDate storeStartDate = (effectiveStartDate != null)
                ? effectiveStartDate
                : (latestBaseDate != null ? latestBaseDate.plusDays(1) : EARLIEST_BASE_DATE);
        if (storeStartDate.isAfter(endDate)) {
            return List.of();
        }

        LocalDate fetchStartDate = storeStartDate.minusDays(200);
        List<DailyStockPriceSnapshot> apiResults = fetchPricesFromPort(stock, fetchStartDate, endDate);
        if (apiResults.isEmpty()) {
            return List.of();
        }

        LocalDate investorFetchStart = endDate.minusDays(RECENT_SUPPLY_DEMAND_DAYS);
        Map<LocalDate, InvestorTradingSnapshot> investorMap = executeKisCall("investor-trading-snapshots", () ->
                        stockPricePort.fetchInvestorTradingSnapshots(stock, investorFetchStart, endDate))
                .stream()
                .collect(Collectors.toMap(InvestorTradingSnapshot::baseDate, value -> value, (left, right) -> left));

        Map<LocalDate, OHLCRecord> mergedData = new TreeMap<>();
        for (StockPrice price : historicalEntities) {
            mergedData.put(price.getId().getBaseDate(), new OHLCRecord(
                    price.getOpenPrice(),
                    price.getHighPrice(),
                    price.getLowPrice(),
                    price.getClosePrice(),
                    price.getVolume(),
                    price.getTransactionAmt(),
                    price.getNetInstitutionalBuyingAmt(),
                    price.getNetForeignBuyingAmt()
            ));
        }

        for (DailyStockPriceSnapshot snapshot : apiResults) {
            InvestorTradingSnapshot investor = investorMap.get(snapshot.baseDate());
            mergedData.put(snapshot.baseDate(), new OHLCRecord(
                    snapshot.openPrice(),
                    snapshot.highPrice(),
                    snapshot.lowPrice(),
                    snapshot.closePrice(),
                    snapshot.volume(),
                    snapshot.transactionAmt(),
                    investor != null ? defaultDecimal(investor.netInstitutionalBuyingAmt()) : BigDecimal.ZERO,
                    investor != null ? defaultDecimal(investor.netForeignBuyingAmt()) : BigDecimal.ZERO
            ));
        }

        List<LocalDate> fullDates = new ArrayList<>(mergedData.keySet());
        List<BigDecimal> fullHighPrices = fullDates.stream().map(date -> mergedData.get(date).high()).toList();
        List<BigDecimal> fullLowPrices = fullDates.stream().map(date -> mergedData.get(date).low()).toList();
        List<BigDecimal> fullClosingPrices = fullDates.stream().map(date -> mergedData.get(date).close()).toList();
        List<TechnicalIndicators> allIndicators = TechnicalIndicatorCalculator.calculateSeries(
                fullHighPrices,
                fullLowPrices,
                fullClosingPrices,
                fullDates
        );

        List<StockPrice> entities = new ArrayList<>();
        for (int i = 0; i < fullDates.size(); i++) {
            LocalDate date = fullDates.get(i);
            if (date.isBefore(storeStartDate)) {
                continue;
            }
            BigDecimal prevClose = (i > 0) ? fullClosingPrices.get(i - 1) : BigDecimal.ZERO;
            OHLCRecord data = mergedData.get(date);
            entities.add(StockPrice.of(
                    stock,
                    date,
                    data.open(),
                    data.high(),
                    data.low(),
                    data.close(),
                    data.close(),
                    prevClose,
                    data.volume(),
                    data.transactionAmt(),
                    data.instBuying(),
                    data.frgnBuying(),
                    allIndicators.get(i)
            ));
        }
        return entities;
    }

    private List<DailyStockPriceSnapshot> fetchPricesFromPort(Stock stock, LocalDate start, LocalDate end) {
        if (!stock.getTicker().matches("^[0-9]+$")) {
            return List.of();
        }

        List<DailyStockPriceSnapshot> allDetails = new ArrayList<>();
        LocalDate cursorDate = end;
        while (!cursorDate.isBefore(start)) {
            LocalDate chunkStartDate = cursorDate.minusDays(CHUNK_DAYS);
            if (chunkStartDate.isBefore(start)) {
                chunkStartDate = start;
            }

            final LocalDate finalChunkStartDate = chunkStartDate;
            final LocalDate finalCursorDate = cursorDate;
            List<DailyStockPriceSnapshot> response = executeKisCall("daily-prices:%s".formatted(stock.getTicker()), () ->
                    stockPricePort.fetchDailyPrices(stock, finalChunkStartDate, finalCursorDate)
            );

            if (response.isEmpty()) {
                cursorDate = chunkStartDate.minusDays(1);
                continue;
            }

            allDetails.addAll(response);
            LocalDate oldestDateInResponse = response.get(response.size() - 1).baseDate();
            cursorDate = oldestDateInResponse.isBefore(cursorDate)
                    ? oldestDateInResponse.minusDays(1)
                    : chunkStartDate.minusDays(1);
        }
        return allDetails;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private <T> T executeKisCall(String operation, Supplier<T> supplier) {
        log.info("[KIS 배치] 호출 준비 operation={}", operation);
        try {
            return supplier.get();
        } catch (KisApiException exception) {
            if (exception.isRateLimitExceeded()) {
                int detectionCount = kisRateLimitDetectionCount.incrementAndGet();
                log.warn("[KIS 배치] KIS 호출 제한 감지 operation={}, detectionCount={}, msgCd={}, msg1={}",
                        operation, detectionCount, exception.msgCd(), exception.msg1());
            }
            throw exception;
        }
    }

    private record OHLCRecord(
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            Long volume,
            BigDecimal transactionAmt,
            BigDecimal instBuying,
            BigDecimal frgnBuying
    ) {
    }
}
