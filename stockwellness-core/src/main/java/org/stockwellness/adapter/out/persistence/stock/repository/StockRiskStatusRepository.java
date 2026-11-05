package org.stockwellness.adapter.out.persistence.stock.repository;

import java.util.Collection;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.stockwellness.domain.stock.StockRiskStatus;

public interface StockRiskStatusRepository extends JpaRepository<StockRiskStatus, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from StockRiskStatus r where r.stockId in :ids")
    List<StockRiskStatus> findAllForUpdate(@Param("ids") Collection<Long> ids);
}
