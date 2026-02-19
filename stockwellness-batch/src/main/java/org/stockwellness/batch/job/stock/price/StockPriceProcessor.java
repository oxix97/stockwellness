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
import org.stockwellness.domain.stock.price.TechnicalIndicatorCalculator;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.price.StockPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class StockPriceProcessor implements ItemProcessor<Stock, List<StockPrice>> {

    private final KisDailyPriceAdapter kisAdapter;
    private final TechnicalIndicatorCalculator indicatorCalculator;

    @Value("#{jobParameters['startDate']}")
    private String startDateStr;

    @Value("#{jobParameters['endDate']}")
    private String endDateStr;

    // API 1회 호출 시 요청할 기간 (KIS 최대 100건이므로 넉넉히 4개월 정도씩 끊음)
    private static final int CHUNK_DAYS = 100;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public List<StockPrice> process(Stock stock) throws Exception {
        // 1. 수집 기간 및 지표 계산을 위한 버퍼 기간 결정
        LocalDate endDate = StringUtils.hasText(endDateStr)
                ? LocalDate.parse(endDateStr, DATE_FMT)
                : LocalDate.now().minusDays(1);

        LocalDate targetStartDate = StringUtils.hasText(startDateStr)
                ? LocalDate.parse(startDateStr, DATE_FMT)
                : endDate;

        // 지표 계산을 위해 250일(약 1년의 영업일) 더 과거부터 API 호출
        // 이는 MA120의 안정적 계산과 EMA 수렴을 위해 필수적임.
        LocalDate fetchLimitDate = targetStartDate.minusDays(250);

        List<KisDailyPriceDetail> allDailyDetails = new ArrayList<>();
        LocalDate cursorDate = endDate;

        // 2. Pagination Logic
        while (!cursorDate.isBefore(fetchLimitDate)) {
            LocalDate fetchStartDate = cursorDate.minusDays(CHUNK_DAYS);
            if (fetchStartDate.isBefore(fetchLimitDate)) {
                fetchStartDate = fetchLimitDate;
            }

            List<KisDailyPriceDetail> response = kisAdapter.fetchDailyPrices(stock, fetchStartDate, cursorDate);
            
            // [WARNING] API Rate Limit 대응
            // 현재는 단일 스레드 배치이므로 Thread.sleep(70)으로 충분하나,
            // 향후 멀티스레드(Step Parallelism) 확장 시 Resilience4j RateLimiter 등으로 교체 필수.
            Thread.sleep(70);

            if (response == null || response.isEmpty()) break;
            allDailyDetails.addAll(response);

            LocalDate oldestDateInResponse = response.get(response.size() - 1).baseDate();
            if (oldestDateInResponse.isBefore(cursorDate)) {
                cursorDate = oldestDateInResponse.minusDays(1);
            } else {
                break;
            }
        }

        if (allDailyDetails.isEmpty()) return null;

        // 3. Sort & Batch Calculate Indicators
        allDailyDetails.sort(Comparator.comparing(KisDailyPriceDetail::baseDate));

        List<BigDecimal> pricestockPrice = allDailyDetails.stream()
                .map(KisDailyPriceDetail::closePrice)
                .toList();
        
        // 전체 시계열 지표를 한 번에 계산 (O(N))
        List<TechnicalIndicators> indicatorsSeries = indicatorCalculator.calculateSeries(pricestockPrice);

        List<StockPrice> entities = new ArrayList<>();

        for (int i = 0; i < allDailyDetails.size(); i++) {
            KisDailyPriceDetail dto = allDailyDetails.get(i);
            TechnicalIndicators indicators = indicatorsSeries.get(i);

            // 실제 저장은 요청받은 targetStartDate 이후 데이터만 수행
            if (!dto.baseDate().isBefore(targetStartDate)) {
                StockPrice entity = StockPrice.of(
                        stock,
                        dto.baseDate(),
                        dto.openPrice(),
                        dto.highPrice(),
                        dto.lowPrice(),
                        dto.closePrice(),
                        dto.closePrice(),
                        dto.volume(),
                        dto.transactionAmt(),
                        indicators
                );
                entities.add(entity);
            }
        }

        if (!entities.isEmpty()) {
            log.info("Successfully processed [{}]: Saved {} rows (Total fetched for indicators: {})",
                    stock.getTicker(), entities.size(), allDailyDetails.size());
        }

        return entities.isEmpty() ? null : entities;
    }
}
