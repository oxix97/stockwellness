package org.stockwellness.adapter.out.persistence.stock.repository;

import java.util.Collection;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.stockwellness.domain.stock.insight.IndexDaily;
import org.stockwellness.domain.stock.insight.IndexDailyId;

public interface IndexDailyRepository extends JpaRepository<IndexDaily, IndexDailyId> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from IndexDaily d where d.id in :ids")
    List<IndexDaily> findAllForUpdate(@Param("ids") Collection<IndexDailyId> ids);
}
