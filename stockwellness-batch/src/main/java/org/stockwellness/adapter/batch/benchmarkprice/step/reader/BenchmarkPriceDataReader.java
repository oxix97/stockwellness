package org.stockwellness.adapter.batch.benchmarkprice.step.reader;

import java.time.LocalDate;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.stockwellness.adapter.batch.benchmarkprice.model.BenchmarkPriceDataWrapper;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.BenchmarkPriceData;
import org.stockwellness.domain.stock.BenchmarkType;

/**
 * [Reader] 모든 BenchmarkType을 순회하며 KIS API로부터 시세를 읽어오는 클래스
 */
@Slf4j
public class BenchmarkPriceDataReader implements ItemReader<BenchmarkPriceDataWrapper> {

    private final KisDailyPriceAdapter kisAdapter;
    private final LocalDate startDate;
    private final LocalDate endDate;

    private final Iterator<BenchmarkType> typeIterator;
    private Iterator<BenchmarkPriceData> dataIterator = Collections.emptyIterator();
    private BenchmarkType currentType;
    private boolean hasReadableData;
    private final List<String> attemptedBenchmarks = new ArrayList<>();
    private final List<String> emptyBenchmarks = new ArrayList<>();
    private final List<String> failedBenchmarks = new ArrayList<>();

    public BenchmarkPriceDataReader(KisDailyPriceAdapter kisAdapter, LocalDate startDate, LocalDate endDate) {
        this.kisAdapter = kisAdapter;
        this.startDate = startDate;
        this.endDate = endDate;
        this.typeIterator = Arrays.asList(BenchmarkType.values()).iterator();
    }

    /**
     * 데이터를 하나씩 읽어 Processor로 넘겨줍니다.
     * 현재 지수의 시세 데이터를 모두 읽으면 다음 지수의 시세를 API로 호출합니다.
     */
    @Override
    public BenchmarkPriceDataWrapper read() {
        while (!dataIterator.hasNext()) {
            if (!typeIterator.hasNext()) {
                if (!hasReadableData) {
                    throw new IllegalStateException(String.format(
                            "지수 시세 동기화 결과가 비어 있습니다. startDate=%s, endDate=%s, attemptedBenchmarks=%s, emptyCount=%d, failedCount=%d, emptyBenchmarks=%s, failedBenchmarks=%s. 영업일 범위와 KIS 응답을 확인하세요.",
                            startDate, endDate, attemptedBenchmarks, emptyBenchmarks.size(), failedBenchmarks.size(), emptyBenchmarks, failedBenchmarks
                    ));
                }
                if (!emptyBenchmarks.isEmpty() || !failedBenchmarks.isEmpty()) {
                    log.warn("[지수 동기화 Reader] 일부 지수는 비었거나 실패했지만 동기화는 계속 진행됩니다. startDate={}, endDate={}, emptyBenchmarks={}, failedBenchmarks={}",
                            startDate, endDate, emptyBenchmarks, failedBenchmarks);
                }
                return null; // 모든 지수와 시세 처리가 완료되면 null 반환 (Batch 종료)
            }
            currentType = typeIterator.next();
            List<BenchmarkPriceData> prices = fetchPrices(currentType);
            if (!prices.isEmpty()) {
                hasReadableData = true;
            }
            dataIterator = prices.iterator();
        }
        return new BenchmarkPriceDataWrapper(currentType, dataIterator.next());
    }

    /**
     * KIS API를 통해 특정 지수의 기간별 시세를 가져옵니다.
     */
    private List<BenchmarkPriceData> fetchPrices(BenchmarkType type) {
        attemptedBenchmarks.add(type.name());
        log.info("[지수 동기화 Reader] 시세 조회 시작: {} (티커: {}, 타입: {})",
                type.getDescription(), type.getTicker(), type.isOverseas() ? "해외" : "국내");

        try {
            List<BenchmarkPriceData> details;
            if (type.isOverseas()) {
                details = kisAdapter.fetchOverseasIndexDailyPrices(type.getTicker(), startDate, endDate);
            } else {
                details = kisAdapter.fetchIndexDailyPrices(type.getTicker(), startDate, endDate);
            }

            if (details == null || details.isEmpty()) {
                emptyBenchmarks.add(type.name());
                log.warn("[지수 동기화 Reader] 수집된 데이터가 없습니다: {} (ticker={}, startDate={}, endDate={})",
                        type.getDescription(), type.getTicker(), startDate, endDate);
                return Collections.emptyList();
            }

            List<BenchmarkPriceData> sorted = details.stream()
                    .sorted(Comparator.comparing(BenchmarkPriceData::baseDate))
                    .toList();

            log.info("[지수 동기화 Reader] 시세 조회 성공: {} (ticker={}, count={}, firstDate={}, lastDate={})",
                    type.getDescription(),
                    type.getTicker(),
                    sorted.size(),
                    sorted.getFirst().baseDate(),
                    sorted.getLast().baseDate());

            return sorted;
        } catch (RuntimeException e) {
            failedBenchmarks.add("%s(%s)".formatted(type.name(), abbreviate(e.getMessage())));
            log.error("[지수 동기화 Reader] 시세 조회 실패: {} (ticker={}, startDate={}, endDate={}): {}",
                    type.getDescription(), type.getTicker(), startDate, endDate, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private String abbreviate(String message) {
        if (message == null || message.isBlank()) {
            return "원인 메시지 없음";
        }
        return message.length() <= 160 ? message : message.substring(0, 157) + "...";
    }
}
