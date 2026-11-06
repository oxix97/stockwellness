package org.stockwellness.adapter.out.persistence.actual;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.stockwellness.domain.actual.ActualAccount;

public interface ActualAccountRepository extends JpaRepository<ActualAccount, Long> {
    List<ActualAccount> findAllByMemberIdOrderByIdAsc(long memberId);
    Optional<ActualAccount> findByIdAndMemberId(long id, long memberId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ActualAccount a where a.id = :id and a.memberId = :memberId")
    Optional<ActualAccount> lockOwned(@Param("id") long id, @Param("memberId") long memberId);
}
