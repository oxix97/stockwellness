package org.stockwellness.adapter.out.persistence.stock.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.stock.insight.MarketIndex;

public interface MarketIndexRepository extends JpaRepository<MarketIndex, Long> {

    Optional<MarketIndex> findByIndexKey(String indexKey);
}
