package org.stockwellness.application.port.out.actual;

import java.util.List;
import java.util.Optional;
import org.stockwellness.domain.actual.ActualAccount;
import org.stockwellness.domain.actual.ActualMutationReceipt;

public interface ActualAccountPort {
    void lockMember(long memberId);
    List<ActualAccount> list(long memberId);
    Optional<ActualAccount> owned(long memberId, long accountId);
    Optional<ActualAccount> lockOwned(long memberId, long accountId);
    ActualAccount saveAndFlush(ActualAccount account);
    Optional<ActualMutationReceipt> receipt(long memberId, long scope, String operation, String key);
    void saveReceipt(ActualMutationReceipt receipt);
}
