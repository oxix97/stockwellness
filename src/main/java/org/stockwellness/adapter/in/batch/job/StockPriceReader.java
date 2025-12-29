package org.stockwellness.adapter.in.batch.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.stockwellness.application.port.out.FetchStockPricePort;
import org.stockwellness.domain.stock.StockHistory;

import java.time.LocalDate;
import java.util.List;

@Slf4j
public class StockPriceReader implements ItemReader<List<StockHistory>> {
    private final FetchStockPricePort stockPricePort;
    private LocalDate currentDate;
    private final LocalDate endDate;

    public StockPriceReader(FetchStockPricePort stockPricePort, LocalDate currentDate, LocalDate endDate) {
        this.stockPricePort = stockPricePort;
        this.currentDate = currentDate;
        this.endDate = endDate;
    }

    @Override
    public List<StockHistory> read() throws Exception {
        // 1. 종료 조건 확인
        if (currentDate.isAfter(endDate)) {
            return null; // 배치 종료 (End of Input)
        }

        log.info(">>> Fetching data for date: {}", currentDate);

        // 2. API 호출 속도 조절 (Throttling)
        Thread.sleep(100);

        // 3. Port를 통해 데이터 조회
        List<StockHistory> histories = null;
        try {
            histories = stockPricePort.fetchDailyPrice(currentDate);
        } catch (Exception e) {
            log.error("API 호출 실패: {}", currentDate, e);
            throw e; // 재시도를 위해 예외 던짐
        }

        // 4. 날짜 증가 (상태 변경)
        currentDate = currentDate.plusDays(1);

        // 5. 데이터가 없으면(휴일 등) 다음 날짜를 위해 재귀 호출하거나 빈 리스트 반환
        if (histories == null || histories.isEmpty()) {
            return read(); // 재귀 호출로 다음 날짜 시도 (주의: 휴일이 길면 스택오버플로우 가능성 있음, 반복문으로 변경 권장)
        }

        return histories;
    }
}