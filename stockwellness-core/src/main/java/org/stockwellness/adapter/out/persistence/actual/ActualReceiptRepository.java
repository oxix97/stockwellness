package org.stockwellness.adapter.out.persistence.actual;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.actual.ActualMutationReceipt;

public interface ActualReceiptRepository extends JpaRepository<ActualMutationReceipt, Long> {
    Optional<ActualMutationReceipt> findByMemberIdAndAccountScopeAndOperationAndRequestKey(
            long memberId, long accountScope, String operation, String requestKey);
}
