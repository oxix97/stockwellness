package org.stockwellness.application.port.in.actual;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.stockwellness.domain.actual.ActualAccount;

public interface ActualAccountUseCase {
    List<Account> list(long memberId);
    Account get(long memberId, long accountId);
    Mutation create(Create command);
    Mutation rename(Rename command);

    record Create(long memberId, String name, String baseCurrency, String brokerLabel, String key) { }
    record Rename(long memberId, long accountId, String name, String ifMatch, String key) { }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    record Account(long accountId, String accountType, String name, String brokerLabel, String baseCurrency,
                   long accountVersion, long ledgerVersion) {
        public static Account from(ActualAccount account) {
            return new Account(account.getId(), "ACTUAL", account.getName(), account.getBrokerLabel(),
                    account.getBaseCurrency(), account.getAccountVersion(), account.getLedgerVersion());
        }
    }
    record Mutation(Account data, LocalDateTime timestamp, boolean created) { }
}
