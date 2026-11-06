package org.stockwellness.domain.actual;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.GlobalException;

@Entity
@Table(name = "actual_account", indexes = @Index(name = "idx_actual_account_owner", columnList = "member_id,id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActualAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, updatable = false) private Long memberId;
    @Column(nullable = false, columnDefinition = "text") private String name;
    @Column(columnDefinition = "text") private String brokerLabel;
    @Column(nullable = false, length = 3, updatable = false) private String baseCurrency;
    @Column(nullable = false) private long accountVersion;
    @Column(nullable = false) private long ledgerVersion;
    @Column(nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(nullable = false) private OffsetDateTime updatedAt;

    public static ActualAccount create(long memberId, String name, String brokerLabel, String currency) {
        if (memberId <= 0 || memberId > ActualAccountRules.MAX_SAFE_INTEGER || !"KRW".equals(currency))
            throw ActualAccountRules.invalidInput();
        ActualAccount account = new ActualAccount();
        account.memberId = memberId;
        account.name = ActualAccountRules.text(name, 50, false);
        account.brokerLabel = ActualAccountRules.text(brokerLabel, 100, true);
        account.baseCurrency = "KRW";
        account.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        account.updatedAt = account.createdAt;
        return account;
    }

    public void rename(String name) {
        String normalized = ActualAccountRules.text(name, 50, false);
        if (accountVersion >= ActualAccountRules.MAX_SAFE_INTEGER)
            throw new GlobalException(ErrorCode.ACTUAL_NUMERIC_RANGE_EXCEEDED);
        this.name = normalized;
        accountVersion++;
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
