package org.stockwellness.application.port.out;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * 특정 종목의 N일치 이동평균선 계산 등을 위해 최근 N건만 가져오기
     * <p>JPQL 사용 예시</p>
     */
    @Query("SELECT h FROM StockHistory h WHERE h.isinCode = :isinCode AND h.baseDate <= :targetDate ORDER BY h.baseDate DESC LIMIT :limit")
    List<StockHistory> findRecentHistory(
            @Param("isinCode") String isinCode,
            @Param("targetDate") LocalDate targetDate,
            @Param("limit") int limit
    );
}
