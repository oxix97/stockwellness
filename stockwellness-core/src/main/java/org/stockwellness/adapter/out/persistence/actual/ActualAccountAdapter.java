package org.stockwellness.adapter.out.persistence.actual;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.stockwellness.application.port.out.actual.ActualAccountPort;
import org.stockwellness.domain.actual.ActualAccount;
import org.stockwellness.domain.actual.ActualMutationReceipt;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.GlobalException;

@Repository
@RequiredArgsConstructor
public class ActualAccountAdapter implements ActualAccountPort {
    private final ActualAccountRepository accounts;
    private final ActualReceiptRepository receipts;
    private final EntityManager entityManager;

    @Override public void lockMember(long memberId) {
        // Creating an account has no account row yet; this lock serializes this owner's create keys.
        if (entityManager.createNativeQuery("select id from member where id = :id for update")
                .setParameter("id", memberId).getResultList().isEmpty())
            throw new GlobalException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    @Override public List<ActualAccount> list(long memberId) { return accounts.findAllByMemberIdOrderByIdAsc(memberId); }
    @Override public Optional<ActualAccount> owned(long memberId, long accountId) {
        return accounts.findByIdAndMemberId(accountId, memberId);
    }
    @Override public Optional<ActualAccount> lockOwned(long memberId, long accountId) {
        return accounts.lockOwned(accountId, memberId);
    }
    @Override public ActualAccount saveAndFlush(ActualAccount account) { return accounts.saveAndFlush(account); }
    @Override public Optional<ActualMutationReceipt> receipt(long memberId, long scope, String op, String key) {
        return receipts.findByMemberIdAndAccountScopeAndOperationAndRequestKey(memberId, scope, op, key);
    }
    @Override public void saveReceipt(ActualMutationReceipt receipt) { receipts.saveAndFlush(receipt); }
}
