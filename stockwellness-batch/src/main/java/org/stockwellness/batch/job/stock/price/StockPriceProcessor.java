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
    private static final LocalDate EARLIEST_BASE_DATE = LocalDate.of(2022, 1, 1);

    @Override
    public List<StockPrice> process(List<Stock> stocks) throws Exception {
        if (stocks == null || stocks.isEmpty()) return null;
        long startTime = System.currentTimeMillis();

        LocalDate endDate = StringUtils.hasText(endDateStr)
                ? DateUtil.parse(endDateStr)
                : DateUtil.today();

        // [추가] 데이터 저장 하한선 (2022-01-01) 준수
        LocalDate paramStartDate = StringUtils.hasText(startDateStr) ? DateUtil.parse(startDateStr) : null;
        LocalDate effectiveStartDate = (paramStartDate != null && paramStartDate.isBefore(EARLIEST_BASE_DATE))
                ? EARLIEST_BASE_DATE : paramStartDate;

        // 파라미터가 명시적으로 있으면 '소급(Backfill) 모드'로 간주
        boolean isExplicitRange = (effectiveStartDate != null);

        Map<Long, LocalDate> latestDatesMap = stockPricePort.findLatestBaseDatesByStocks(stocks);

        // 지표 계산을 위한 과거 데이터 로드 (고/저가 포함 엔티티 리스트)
        // [수정] effectiveStartDate를 기준으로 버퍼 로드
        LocalDate lookbackBaseDate = isExplicitRange ? effectiveStartDate : endDate;
        Map<Long, List<StockPrice>> historicalEntitiesMap = stockPricePort.findRecentPricesWithDateByStocks(stocks,
                lookbackBaseDate, INDICATOR_BUFFER_DAYS);

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
            try {
                resultEntities.addAll(processMultiStockPrices(todaySyncStocks, endDate, historicalEntitiesMap));
            } catch (Exception e) {
                log.error("Failed to process multi stock prices for {} stocks: {}", todaySyncStocks.size(), e.getMessage());
            }
        }

        for (Stock stock : gapSyncStocks) {
            try {
                resultEntities.addAll(processIndividualGap(stock, latestDatesMap.get(stock.getId()), effectiveStartDate, endDate, historicalEntitiesMap.getOrDefault(stock.getId(), Collections.emptyList())));
            } catch (io.github.resilience4j.ratelimiter.RequestNotPermitted e) {
                log.warn("Rate limit exceeded for stock {}", stock.getTicker());
            } catch (io.github.resilience4j.core.exception.AcquirePermissionCancelledException e) {
                log.info("Batch process interrupted during shutdown for stock {}. Stopping current task.", stock.getTicker());
                throw new BatchException(ErrorCode.RATE_LIMIT_WAIT_CANCELLED);
            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) {
                    log.info("Thread interrupted during processing stock {}. Stopping.", stock.getTicker());
                    throw new BatchException(ErrorCode.BATCH_STEP_INTERRUPTED);
                }
                log.error("Critical error processing individual gap for stock {}: {}", stock.getTicker(), e.getMessage(), e);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Batch Processed ({}ms): [Total: {}] TodaySync: {}, RangeSync: {}, Created Entities: {}",
                duration, stocks.size(), todaySyncStocks.size(), gapSyncStocks.size(), resultEntities.size());

        return resultEntities.isEmpty() ? null : resultEntities;
    }

    private List<StockPrice> processMultiStockPrices(List<Stock> stocks, LocalDate today, Map<Long, List<StockPrice>> historicalEntitiesMap) {
        // [중요] 2022-01-01 이전이면 무시
        if (today.isBefore(EARLIEST_BASE_DATE)) return Collections.emptyList();

        List<String> tickers = stocks.stream().map(Stock::getTicker).toList();

        // RateLimiter 적용
        List<KisMultiStockPriceDetail> apiResults;
        try {
            apiResults = kisRateLimiter.executeSupplier(() -> kisAdapter.fetchMultiStockPrices(tickers));
        } catch (Exception e) {
            log.error("API fetch failed for multi stock prices: {}", e.getMessage());
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
                BigDecimal lastPastClose = pastEntities.isEmpty() ? BigDecimal.ZERO : pastEntities.get(pastEntities.size() - 1).getClosePrice();
                BigDecimal prevClose = (lastPastClose != null) ? lastPastClose : BigDecimal.ZERO;

                List<BigDecimal> highPrices = new ArrayList<>(pastEntities.stream().map(p -> p.getHighPrice() != null ? p.getHighPrice() : p.getClosePrice()).toList());
                List<BigDecimal> lowPrices = new ArrayList<>(pastEntities.stream().map(p -> p.getLowPrice() != null ? p.getLowPrice() : p.getClosePrice()).toList());
                List<BigDecimal> closePrices = new ArrayList<>(pastEntities.stream().map(p -> p.getClosePrice() != null ? p.getClosePrice() : BigDecimal.ZERO).toList());
                List<LocalDate> dates = new ArrayList<>(pastEntities.stream().map(p -> p.getId().getBaseDate()).toList());

                highPrices.add(new BigDecimal(todayPrice.highPrice()));
                lowPrices.add(new BigDecimal(todayPrice.lowPrice()));
                closePrices.add(currentPrice);
                dates.add(today);

                // OHLC 데이터와 날짜를 사용하여 최신 지표 계산
                List<TechnicalIndicators> indicatorSeries = TechnicalIndicatorCalculator.calculateSeries(highPrices, lowPrices, closePrices, dates);
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
            } catch (Exception e) {
                log.error("Failed to process multi-price for stock {}: {}", stock.getTicker(), e.getMessage());
            }
        }
        return entities;
    }

    private List<StockPrice> processIndividualGap(Stock stock, LocalDate latestBaseDate, LocalDate effectiveStartDate, LocalDate endDate, List<StockPrice> historicalEntities) throws InterruptedException {
        // 1. 데이터 저장 시작일 결정
        LocalDate storeStartDate;
        if (effectiveStartDate != null) {
            storeStartDate = effectiveStartDate;
        } else {
            storeStartDate = (latestBaseDate != null) ? latestBaseDate.plusDays(1) : EARLIEST_BASE_DATE;
        }

        if (storeStartDate.isAfter(endDate)) return Collections.emptyList();

        // 2. [핵심] 지표 계산의 정확도를 위해 API 조회 시작일(fetchStartDate)은 storeStartDate보다 200일 앞당김
        LocalDate fetchStartDate = storeStartDate.minusDays(200);

        // 3. API 호출
        List<KisDailyPriceDetail> apiResults = fetchPricesFromKis(stock, fetchStartDate, endDate);
        if (apiResults.isEmpty()) return Collections.emptyList();

        // 4. [개선] TreeMap을 사용하여 날짜 중복 제거 및 정렬 보장
        Map<LocalDate, OHLCRecord> mergedData = new TreeMap<>();

        // DB 데이터 먼저 채우기
        for (StockPrice p : historicalEntities) {
            mergedData.put(p.getId().getBaseDate(), new OHLCRecord(
                    p.getOpenPrice(), p.getHighPrice(), p.getLowPrice(), p.getClosePrice(), p.getVolume(), p.getTransactionAmt()));
        }

        // API 데이터로 덮어쓰기 (최신 정보 우선)
        for (KisDailyPriceDetail dto : apiResults) {
            mergedData.put(dto.baseDate(), new OHLCRecord(
                    dto.openPrice(), dto.highPrice(), dto.lowPrice(), dto.closePrice(), dto.volume(), dto.transactionAmt()));
        }

        List<LocalDate> fullDates = new ArrayList<>(mergedData.keySet());
        List<BigDecimal> fullHighPrices = fullDates.stream().map(d -> mergedData.get(d).high()).toList();
        List<BigDecimal> fullLowPrices = fullDates.stream().map(d -> mergedData.get(d).low()).toList();
        List<BigDecimal> fullClosingPrices = fullDates.stream().map(d -> mergedData.get(d).close()).toList();

        // 5. 전체 기간 지표 계산
        List<TechnicalIndicators> allIndicators = TechnicalIndicatorCalculator.calculateSeries(fullHighPrices, fullLowPrices, fullClosingPrices, fullDates);

        // 6. 저장용 엔티티 필터링 (storeStartDate 이후 데이터만)
        List<StockPrice> entities = new ArrayList<>();
        for (int i = 0; i < fullDates.size(); i++) {
            LocalDate date = fullDates.get(i);
            if (date.isBefore(storeStartDate)) continue;

            BigDecimal prevClose = (i > 0) ? fullClosingPrices.get(i - 1) : BigDecimal.ZERO;
            OHLCRecord data = mergedData.get(date);

            entities.add(StockPrice.of(
                    stock, date, data.open(), data.high(), data.low(),
                    data.close(), data.close(), prevClose, data.volume(), data.transactionAmt(),
                    allIndicators.get(i)
            ));
        }
        return entities;
    }

    private List<KisDailyPriceDetail> fetchPricesFromKis(Stock stock, LocalDate start, LocalDate end) {
        if (!stock.getTicker().matches("^[0-9]+$")) {
            return Collections.emptyList();
        }
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

            if (response.isEmpty()) {
                log.warn("Empty response for {} between {} and {}. Continuing to older dates...",
                        stock.getTicker(), chunkStartDate, cursorDate);
                cursorDate = chunkStartDate.minusDays(1);
                continue;
            }

            allDetails.addAll(response);
            LocalDate oldestDateInResponse = response.get(response.size() - 1).baseDate();

            // 응답받은 데이터 중 가장 오래된 날짜의 이전 날짜로 커서 이동
            if (oldestDateInResponse.isBefore(cursorDate)) {
                cursorDate = oldestDateInResponse.minusDays(1);
            } else {
                // 더 이상 가져올 데이터가 없으면 종료 (중복 방지)
                cursorDate = chunkStartDate.minusDays(1);
            }
        }
        return allDetails;
    }

    private record OHLCRecord(
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            Long volume,
            BigDecimal transactionAmt
    ) {
    }
}
