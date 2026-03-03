package org.stockwellness.adapter.out.persistence.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.stock.insight.MarketIndex;

import java.util.Map;
import java.util.stream.Collectors;

public interface MarketIndexRepository extends JpaRepository<MarketIndex, Long> {

    /**
     * 활성 지수 전체를 {@code Map<indexCode, MarketIndex>}로 반환.
     *
     * <p>Job 시작 시 한 번만 호출하여 Processor에 전달합니다.
     * indexCode = KIS 마스터의 지수업종 중분류 코드 (e.g. "0027" → 제약)
     */
    default Map<String, MarketIndex> findActiveIndexMap() {
        return findAll().stream()
                .collect(Collectors.toMap(MarketIndex::getIndexCode, m -> m));
    }
}