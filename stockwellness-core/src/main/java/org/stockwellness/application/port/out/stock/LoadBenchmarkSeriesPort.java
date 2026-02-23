package org.stockwellness.application.port.out.stock;

import org.stockwellness.application.port.in.stock.result.StockPriceResult;
import java.time.LocalDate;
import java.util.List;

/**
 * 캘린더 정렬을 지원하는 벤치마크 지수 시리즈 조회를 위한 출력 포트
 */
public interface LoadBenchmarkSeriesPort {

    /**
     * 특정 벤치마크 티커에 대해 지정된 날짜 범위의 가격 시리즈를 조회합니다.
     * 주식 데이터와 정렬을 위해 필요한 경우 휴장일 처리가 포함될 수 있습니다.
     */
    List<StockPriceResult> loadBenchmarkSeries(String benchmarkTicker, LocalDate start, LocalDate end);
}
