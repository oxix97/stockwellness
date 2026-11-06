package org.stockwellness.domain.actual;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "actual_mutation_receipt", uniqueConstraints = @UniqueConstraint(name = "uq_actual_receipt_scope",
        columnNames = {"member_id", "account_scope", "operation", "request_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActualMutationReceipt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, updatable = false) private long memberId;
    @Column(nullable = false, updatable = false) private long accountScope;
    @Column(nullable = false, updatable = false) private long responseAccountId;
    @Column(nullable = false, length = 32, updatable = false) private String operation;
    @Column(nullable = false, length = 64, updatable = false) private String requestKey;
    @Column(nullable = false, length = 64, updatable = false) private String requestFingerprint;
    @Column(nullable = false, columnDefinition = "text", updatable = false) private String responseBody;
    @Column(nullable = false, updatable = false) private OffsetDateTime createdAt;

    public static ActualMutationReceipt success(long memberId, long scope, long responseAccountId,
            String operation, String key, String fingerprint, String responseBody) {
        ActualMutationReceipt result = new ActualMutationReceipt();
        result.memberId = memberId; result.accountScope = scope; result.responseAccountId = responseAccountId;
        result.operation = operation; result.requestKey = key; result.requestFingerprint = fingerprint;
        result.responseBody = responseBody; result.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        return result;
    }
}
