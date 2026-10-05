package org.stockwellness.adapter.out.persistence.stock.repository;

import java.util.Collection;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.stockwellness.domain.stock.price.EodStockPrice;
import org.stockwellness.domain.stock.price.EodStockPriceId;

public interface EodStockPriceRepository extends JpaRepository<EodStockPrice, EodStockPriceId> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from EodStockPrice p where p.id in :ids")
    List<EodStockPrice> findAllForUpdate(@Param("ids") Collection<EodStockPriceId> ids);
}
