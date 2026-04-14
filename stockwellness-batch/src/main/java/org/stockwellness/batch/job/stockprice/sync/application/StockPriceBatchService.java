package org.stockwellness.batch.job.stockprice.sync.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.stockwellness.adapter.out.external.kis.dto.KisMultiStockPriceDetail;
import org.stockwellness.adapter.out.external.kis.exception.KisApiException;
import org.stockwellness.application.port.in.batch.StockPriceRepairUseCase;
import org.stockwellness.application.port.in.batch.StockPriceSyncUseCase;
import org.stockwellness.application.port.out.stock.DailyStockPriceSnapshot;
import org.stockwellness.application.port.out.stock.InvestorTradingSnapshot;
import org.stockwellness.application.port.out.stock.StockPricePort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.analysis.TechnicalIndicatorCalculator;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;
import org.stockwellness.global.util.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
        // 기존 하위 호환성을 위해 유지 (필요 시 제거 가능)
        StockPriceSyncResult fetchResult = fetch(command);
        return calculateIndicators(new StockPriceBatchCommand(command.stocks(), command.startDate(), command.endDate()));
    }

    /**
     * [Step 2-1] 시세 수집: 지표 계산 없이 원시 데이터만 수집하여 반환
     */
    @Override
    public StockPriceSyncResult fetch(StockPriceBatchCommand command) {
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

        List<Stock> todaySyncStocks = new ArrayList<>();
        List<Stock> gapSyncStocks = new ArrayList<>();

        for (Stock stock : stocks) {
            LocalDate lastDate = latestDatesMap.get(stock.getId());
            if (!isExplicitRange && lastDate != null
                    && (DateUtil.daysBetween(lastDate, endDate) == 1 || lastDate.isEqual(endDate))) {
                todaySyncStocks.add(stock);
            } else {
                gapSyncStocks.add(stock);
            }
        }

        List<StockPrice> resultEntities = new ArrayList<>();
        if (!todaySyncStocks.isEmpty()) {
            resultEntities.addAll(fetchMultiStockPricesOnly(todaySyncStocks, endDate));
        }

        for (Stock stock : gapSyncStocks) {
            resultEntities.addAll(fetchIndividualGapOnly(
                    stock,
                    latestDatesMap.get(stock.getId()),
                    effectiveStartDate,
                    endDate
            ));
        }

        return new StockPriceSyncResult(resultEntities);
    }

    /**
     * [Step 2-2] 지표 계산: 수집된 데이터를 바탕으로 벌크 조회 후 지표 계산
     */
    @Override
    public StockPriceSyncResult calculateIndicators(StockPriceBatchCommand command) {
        List<Stock> stocks = command.stocks();
        if (stocks == null || stocks.isEmpty()) {
            return new StockPriceSyncResult(List.of());
        }

        LocalDate endDate = StringUtils.hasText(command.endDate()) ? DateUtil.parse(command.endDate()) : DateUtil.today();
        LocalDate paramStartDate = StringUtils.hasText(command.startDate()) ? DateUtil.parse(command.startDate()) : null;
        LocalDate effectiveStartDate = (paramStartDate != null && paramStartDate.isBefore(EARLIEST_BASE_DATE))
                ? EARLIEST_BASE_DATE
                : paramStartDate;

        // 벌크 조회: 300종목의 120일치 데이터를 단 한 번의 쿼리로 로드
        LocalDate lookbackBaseDate = effectiveStartDate != null ? effectiveStartDate : endDate;
        Map<Long, List<StockPrice>> historicalEntitiesMap = stockPricePort.findRecentPricesWithDateByStocks(
                stocks,
                lookbackBaseDate,
                INDICATOR_BUFFER_DAYS + 10 // 수집된 오늘 데이터까지 포함하기 위해 여유분 추가
        );

        List<StockPrice> updatedEntities = new ArrayList<>();
        for (Stock stock : stocks) {
            List<StockPrice> allPrices = historicalEntitiesMap.getOrDefault(stock.getId(), Collections.emptyList());
            if (allPrices.isEmpty()) continue;

            // 날짜순 정렬 보장
            allPrices.sort(Comparator.comparing(p -> p.getId().getBaseDate()));

            List<LocalDate> dates = allPrices.stream().map(p -> p.getId().getBaseDate()).toList();
            List<BigDecimal> highPrices = allPrices.stream().map(p -> p.getHighPrice() != null ? p.getHighPrice() : p.getClosePrice()).toList();
            List<BigDecimal> lowPrices = allPrices.stream().map(p -> p.getLowPrice() != null ? p.getLowPrice() : p.getClosePrice()).toList();
            List<BigDecimal> closePrices = allPrices.stream().map(p -> p.getClosePrice() != null ? p.getClosePrice() : BigDecimal.ZERO).toList();

            List<TechnicalIndicators> indicatorsList = TechnicalIndicatorCalculator.calculateSeries(highPrices, lowPrices, closePrices, dates);

            for (int i = 0; i < allPrices.size(); i++) {
                StockPrice price = allPrices.get(i);
                // 오늘 또는 수집 범위 내의 데이터만 지표 업데이트 (과거 데이터는 이미 계산되어 있을 것이므로 성능상 선택적 업데이트)
                if (price.getId().getBaseDate().isEqual(endDate) || (effectiveStartDate != null && !price.getId().getBaseDate().isBefore(effectiveStartDate))) {
                    BigDecimal prevClose = (i > 0) ? allPrices.get(i - 1).getClosePrice() : BigDecimal.ZERO;
                    price.updateIndicators(indicatorsList.get(i), prevClose);
                    updatedEntities.add(price);
                }
            }
        }

        return new StockPriceSyncResult(updatedEntities);
    }

    private List<StockPrice> fetchMultiStockPricesOnly(List<Stock> stocks, LocalDate today) {
        if (today.isBefore(EARLIEST_BASE_DATE)) return List.of();

        List<KisMultiStockPriceDetail> apiResults = executeKisCall(
                KisCallContext.of("multi-stock-prices", stocks.stream().map(Stock::getTicker).toList(), null, today),
                () -> stockPricePort.fetchMultiStockPrices(stocks.stream().map(Stock::getTicker).toList())
        );
        Map<String, KisMultiStockPriceDetail> apiResultMap = apiResults.stream()
                .collect(Collectors.toMap(KisMultiStockPriceDetail::ticker, v -> v));

        List<StockPrice> entities = new ArrayList<>();
        for (Stock stock : stocks) {
            KisMultiStockPriceDetail todayPrice = apiResultMap.get(stock.getTicker());
            if (todayPrice == null || todayPrice.closePrice() == null || todayPrice.closePrice().compareTo(BigDecimal.ZERO) <= 0) continue;

            entities.add(StockPrice.of(
                    stock, today,
                    todayPrice.openPrice(), todayPrice.highPrice(), todayPrice.lowPrice(), todayPrice.closePrice(),
                    todayPrice.closePrice(), BigDecimal.ZERO, // prevClose는 Indicator 단계에서 보정
                    todayPrice.accumulatedVolume(), todayPrice.accumulatedTradingValue(),
                    TechnicalIndicators.empty()
            ));
        }
        return entities;
    }

    private List<StockPrice> fetchIndividualGapOnly(Stock stock, LocalDate latestBaseDate, LocalDate effectiveStartDate, LocalDate endDate) {
        LocalDate storeStartDate = (effectiveStartDate != null) ? effectiveStartDate : (latestBaseDate != null ? latestBaseDate.plusDays(1) : EARLIEST_BASE_DATE);
        if (storeStartDate.isAfter(endDate)) return List.of();

        List<DailyStockPriceSnapshot> apiResults = fetchPricesFromPort(stock, storeStartDate, endDate);
        if (apiResults.isEmpty()) return List.of();

        return apiResults.stream()
                .map(snapshot -> StockPrice.of(
                        stock, snapshot.baseDate(),
                        snapshot.openPrice(), snapshot.highPrice(), snapshot.lowPrice(), snapshot.closePrice(),
                        snapshot.closePrice(), BigDecimal.ZERO,
                        snapshot.volume(), snapshot.transactionAmt(),
                        TechnicalIndicators.empty()
                )).toList();
    }

    @Override
    public StockPriceRepairResult repair(StockPriceRepairCommand command) {
        List<StockPrice> allPrices = stockPricePort.findRecentPricesWithDateByStocks(
                List.of(command.stock()),
                LocalDate.now(),
                Integer.MAX_VALUE
        ).getOrDefault(command.stock().getId(), List.of());
        if (allPrices.isEmpty()) return new StockPriceRepairResult(List.of());

        LocalDate reqStart = DateUtil.parse(command.startDate());
        LocalDate reqEnd = DateUtil.parse(command.endDate());
        List<StockPriceRepairRow> toUpdate = new ArrayList<>();
        BigDecimal previousClose = null;

        for (StockPrice current : allPrices) {
            LocalDate currentBaseDate = current.getId().getBaseDate();
            if (previousClose != null) {
                boolean inRange = DateUtil.isBetween(currentBaseDate, reqStart, reqEnd);
                boolean needsRepair = current.getPreviousClosePrice() == null || current.getPreviousClosePrice().compareTo(BigDecimal.ZERO) == 0;
                if (inRange && needsRepair) {
                    toUpdate.add(new StockPriceRepairRow(command.stock().getId(), currentBaseDate, previousClose));
                }
            }
            previousClose = current.getClosePrice();
        }
        return new StockPriceRepairResult(toUpdate);
    }

    private List<DailyStockPriceSnapshot> fetchPricesFromPort(Stock stock, LocalDate start, LocalDate end) {
        if (!stock.getTicker().matches("^[0-9]+$")) return List.of();

        List<DailyStockPriceSnapshot> allDetails = new ArrayList<>();
        LocalDate cursorDate = end;
        while (!cursorDate.isBefore(start)) {
            LocalDate chunkStartDate = cursorDate.minusDays(CHUNK_DAYS);
            if (chunkStartDate.isBefore(start)) chunkStartDate = start;

            final LocalDate fStart = chunkStartDate;
            final LocalDate fEnd = cursorDate;
            List<DailyStockPriceSnapshot> response = executeKisCall(
                    KisCallContext.of("daily-prices", stock.getTicker(), fStart, fEnd),
                    () -> stockPricePort.fetchDailyPrices(stock, fStart, fEnd)
            );

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

    private <T> T executeKisCall(KisCallContext context, Supplier<T> supplier) {
        log.info("[KIS 배치] 호출 준비 operation={}, targetCount={}, tickers={}, startDate={}, endDate={}",
                context.operation(), context.targetCount(), context.tickers(), context.startDate(), context.endDate());

        int retryCount = 0;
        int maxRetries = 2;

        while (true) {
            try {
                return supplier.get();
            } catch (KisApiException exception) {
                if (exception.isRateLimitExceeded() && retryCount < maxRetries) {
                    retryCount++;
                    int detectionCount = kisRateLimitDetectionCount.incrementAndGet();
                    log.warn("[KIS 배치] KIS 호출 제한 감지. {}ms 대기 후 재시도합니다. ({} / {}) operation={}, detectionCount={}, msg1={}",
                            1000, retryCount, maxRetries, context.operation(), detectionCount, exception.msg1());
                    try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw exception; }
                    continue;
                }
                throw exception;
            }
        }
    }

    private record OHLCRecord(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, Long volume, BigDecimal transactionAmt, BigDecimal instBuying, BigDecimal frgnBuying, BigDecimal prsnBuying) {}

    private record KisCallContext(String operation, List<String> tickers, LocalDate startDate, LocalDate endDate) {
        private static KisCallContext of(String operation, String ticker, LocalDate startDate, LocalDate endDate) { return new KisCallContext(operation, List.of(ticker), startDate, endDate); }
        private static KisCallContext of(String operation, List<String> tickers, LocalDate startDate, LocalDate endDate) { return new KisCallContext(operation, List.copyOf(tickers), startDate, endDate); }
        private int targetCount() { return tickers.size(); }
    }
}
