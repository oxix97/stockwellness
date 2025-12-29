package org.stockwellness.application.port.out;

import org.stockwellness.domain.stock.StockHistory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * StockHistory 엔티티(시세 정보) 접근을 위한 Repository
 * 복합키(StockHistoryId)를 사용합니다.
 */
public interface StockHistoryRepository {
    /**
     * 특정 종목의 특정 기간 시세 조회 (AI 학습 및 차트용 핵심 쿼리)
     * <p>BaseDate 기준으로 정렬하여 반환</p>
     */
    List<StockHistory> findByIsinCodeAndBaseDateBetweenOrderByBaseDateAsc(
            String isinCode,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * 특정 종목의 가장 최근 데이터 조회
     * <p>데이터 최신화 여부 확인용 (Last Updated Check)</p>
     */
    Optional<StockHistory> findTopByIsinCodeOrderByBaseDateDesc(String isinCode);

    /**
     * 특정 날짜의 전 종목 시세 조회
     * <p>시장 전체 분석(Cross-sectional analysis)용</p>
     */
    List<StockHistory> findByBaseDate(LocalDate baseDate);

    List<StockHistory> findRecentHistory(
            String isinCode,
            LocalDate targetDate,
            int limit
    );

    void saveAll(List<StockHistory> histories);

    void bulkInsert(List<StockHistory> histories);

    List<StockHistory> findTop60ByIsinCodeAndBaseDateBeforeOrderByBaseDateAsc(String isinCode, LocalDate baseDate);

    List<StockHistory> findAllByIsinCodeOrderByBaseDateAsc(String isinCode);

    List<String> findAllIsinCodes();
}
