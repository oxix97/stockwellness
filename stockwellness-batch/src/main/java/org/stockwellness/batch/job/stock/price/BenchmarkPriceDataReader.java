package org.stockwellness.batch.job.stock.price;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.stockwellness.adapter.out.external.kis.adapter.KisDailyPriceAdapter;
import org.stockwellness.adapter.out.external.kis.dto.BenchmarkPriceData;
import org.stockwellness.domain.stock.BenchmarkType;

import java.time.LocalDate;
import java.util.*;

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
                return null; // 모든 지수와 시세 처리가 완료되면 null 반환 (Batch 종료)
            }
            currentType = typeIterator.next();
            dataIterator = fetchPrices(currentType).iterator();
        }
        return new BenchmarkPriceDataWrapper(currentType, dataIterator.next());
    }

    /**
     * KIS API를 통해 특정 지수의 기간별 시세를 가져옵니다.
     */
    private List<BenchmarkPriceData> fetchPrices(BenchmarkType type) {
        log.info("[지수 동기화 Reader] 시세 조회 시작: {} (티커: {}, 타입: {})",
                type.getDescription(), type.getTicker(), type.isOverseas() ? "해외" : "국내");

        List<BenchmarkPriceData> details;
        if (type.isOverseas()) {
            // 해외 지수용 API 호출
            details = kisAdapter.fetchOverseasIndexDailyPrices(type.getTicker(), startDate, endDate);
        } else {
            // 국내 지수용 API 호출
            details = kisAdapter.fetchIndexDailyPrices(type.getTicker(), startDate, endDate);
        }

        if (details == null || details.isEmpty()) {
            log.warn("[지수 동기화 Reader] 수집된 데이터가 없습니다: {}", type.getDescription());
            return Collections.emptyList();
        }

        // Processor에서 순차적 등락률 계산을 위해 날짜 오름차순으로 정렬하여 반환
        return details.stream()
                .sorted(Comparator.comparing(BenchmarkPriceData::baseDate))
                .toList();
    }
}
