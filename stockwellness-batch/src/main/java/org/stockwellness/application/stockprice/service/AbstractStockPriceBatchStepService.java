//package org.stockwellness.application.stockprice.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.util.StringUtils;
//import org.stockwellness.adapter.out.external.kis.exception.KisApiException;
//import org.stockwellness.application.port.in.batch.StockPriceSyncUseCase;
//import org.stockwellness.application.port.out.stock.DailyStockPriceSnapshot;
//import org.stockwellness.application.port.out.stock.StockPricePort;
//import org.stockwellness.domain.stock.Stock;
//import org.stockwellness.domain.stock.price.StockPrice;
//import org.stockwellness.global.util.DateUtil;
//
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.function.Supplier;
//
//@Slf4j
//@RequiredArgsConstructor
//abstract class AbstractStockPriceBatchStepService {
//
//    protected static final int CHUNK_DAYS = 100;
//    protected static final int INDICATOR_BUFFER_DAYS = 120;
//    protected static final LocalDate EARLIEST_BASE_DATE = LocalDate.of(2022, 1, 1);
//
//    protected final StockPricePort stockPricePort;
//    private final AtomicInteger kisRateLimitDetectionCount = new AtomicInteger();
//
//    protected BatchDateContext resolveBatchDateContext(StockPriceSyncUseCase.StockPriceBatchCommand command) {
//        LocalDate effectiveBusinessDate = StringUtils.hasText(command.endDate())
//                ? DateUtil.parse(command.endDate())
//                : currentDate();
//        LocalDate effectiveStartDate = resolveEffectiveStartDate(command.startDate());
//        return new BatchDateContext(
//                effectiveBusinessDate,
//                effectiveStartDate,
//                effectiveBusinessDate.equals(currentDate())
//        );
//    }
//
//    protected LocalDate resolveStoreStartDate(LocalDate latestBaseDate, LocalDate effectiveStartDate, LocalDate endDate) {
//        LocalDate storeStartDate = (effectiveStartDate != null)
//                ? effectiveStartDate
//                : (latestBaseDate != null ? latestBaseDate.plusDays(1) : EARLIEST_BASE_DATE);
//        return storeStartDate.isBefore(EARLIEST_BASE_DATE) ? EARLIEST_BASE_DATE : storeStartDate;
//    }
//
//    protected StockSyncPartition partitionStocks(List<Stock> stocks, BatchDateContext dateContext) {
//        Map<Long, LocalDate> latestDatesMap = stockPricePort.findLatestBaseDatesByStocks(stocks);
//        List<Stock> todaySyncStocks = new ArrayList<>();
//        List<Stock> gapSyncStocks = new ArrayList<>();
//
//        for (Stock stock : stocks) {
//            LocalDate lastDate = latestDatesMap.get(stock.getId());
//            if (dateContext.useMultiPriceApi() && isUpToDateForBusinessDate(lastDate, dateContext.effectiveBusinessDate())) {
//                todaySyncStocks.add(stock);
//            } else {
//                gapSyncStocks.add(stock);
//            }
//        }
//
//        return new StockSyncPartition(todaySyncStocks, gapSyncStocks, latestDatesMap);
//    }
//
//    protected boolean shouldUpdateIndicators(StockPrice price, BatchDateContext dateContext) {
//        return price.getId().getBaseDate().isEqual(dateContext.effectiveBusinessDate())
//                || (dateContext.effectiveStartDate() != null
//                && !price.getId().getBaseDate().isBefore(dateContext.effectiveStartDate()));
//    }
//
//    protected List<DailyStockPriceSnapshot> fetchPricesFromPort(Stock stock, LocalDate start, LocalDate end) {
//        if (!stock.getTicker().matches("^[0-9]+$")) {
//            return List.of();
//        }
//
//        List<DailyStockPriceSnapshot> allDetails = new ArrayList<>();
//        LocalDate cursorDate = end;
//        while (!cursorDate.isBefore(start)) {
//            LocalDate chunkStartDate = cursorDate.minusDays(CHUNK_DAYS);
//            if (chunkStartDate.isBefore(start)) {
//                chunkStartDate = start;
//            }
//
//            LocalDate finalChunkStartDate = chunkStartDate;
//            LocalDate finalCursorDate = cursorDate;
//            List<DailyStockPriceSnapshot> response = executeKisCall(
//                    KisCallContext.of("daily-prices", stock.getTicker(), finalChunkStartDate, finalCursorDate),
//                    () -> stockPricePort.fetchDailyPrices(stock, finalChunkStartDate, finalCursorDate)
//            );
//
//            if (response.isEmpty()) {
//                cursorDate = chunkStartDate.minusDays(1);
//                continue;
//            }
//
//            allDetails.addAll(response);
//            LocalDate oldestDateInResponse = response.getLast().baseDate();
//            cursorDate = oldestDateInResponse.isBefore(cursorDate)
//                    ? oldestDateInResponse.minusDays(1)
//                    : chunkStartDate.minusDays(1);
//        }
//        return allDetails;
//    }
//
//    protected <T> T executeKisCall(KisCallContext context, Supplier<T> supplier) {
//        log.info("[KIS 배치] 호출 준비 operation={}, targetCount={}, tickers={}, startDate={}, endDate={}",
//                context.operation(), context.targetCount(), context.tickers(), context.startDate(), context.endDate());
//
//        int retryCount = 0;
//        int maxRetries = 2;
//
//        while (true) {
//            try {
//                return supplier.get();
//            } catch (KisApiException exception) {
//                if (exception.isRateLimitExceeded() && retryCount < maxRetries) {
//                    retryCount++;
//                    int detectionCount = kisRateLimitDetectionCount.incrementAndGet();
//                    log.warn("[KIS 배치] KIS 호출 제한 감지. {}ms 대기 후 재시도합니다. ({} / {}) operation={}, detectionCount={}, msg1={}",
//                            1000, retryCount, maxRetries, context.operation(), detectionCount, exception.msg1());
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException interruptedException) {
//                        Thread.currentThread().interrupt();
//                        throw exception;
//                    }
//                    continue;
//                }
//                throw exception;
//            }
//        }
//    }
//
//    protected boolean isEmptyStocks(List<Stock> stocks) {
//        return stocks == null || stocks.isEmpty();
//    }
//
//    protected LocalDate currentDate() {
//        return LocalDate.now();
//    }
//
//    private LocalDate resolveEffectiveStartDate(String startDate) {
//        if (!StringUtils.hasText(startDate)) {
//            return null;
//        }
//        LocalDate parsedStartDate = DateUtil.parse(startDate);
//        return parsedStartDate.isBefore(EARLIEST_BASE_DATE) ? EARLIEST_BASE_DATE : parsedStartDate;
//    }
//
//    private boolean isUpToDateForBusinessDate(LocalDate lastDate, LocalDate effectiveBusinessDate) {
//        return lastDate != null
//                && (DateUtil.daysBetween(lastDate, effectiveBusinessDate) == 1 || lastDate.isEqual(effectiveBusinessDate));
//    }
//
//    protected record KisCallContext(String operation, List<String> tickers, LocalDate startDate, LocalDate endDate) {
//        protected static KisCallContext of(String operation, String ticker, LocalDate startDate, LocalDate endDate) {
//            return new KisCallContext(operation, List.of(ticker), startDate, endDate);
//        }
//
//        private int targetCount() {
//            return tickers.size();
//        }
//    }
//
//    protected record BatchDateContext(
//            LocalDate effectiveBusinessDate,
//            LocalDate effectiveStartDate,
//            boolean useMultiPriceApi
//    ) {
//    }
//
//    protected record StockSyncPartition(
//            List<Stock> todaySyncStocks,
//            List<Stock> gapSyncStocks,
//            Map<Long, LocalDate> latestDatesMap
//    ) {
//    }
//}
