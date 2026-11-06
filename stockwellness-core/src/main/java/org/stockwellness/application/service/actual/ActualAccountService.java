package org.stockwellness.application.service.actual;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.actual.ActualAccountUseCase;
import org.stockwellness.application.port.out.actual.ActualAccountPort;
import org.stockwellness.domain.actual.ActualAccount;
import org.stockwellness.domain.actual.ActualAccountRules;
import org.stockwellness.domain.actual.ActualMutationReceipt;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.GlobalException;

@Service
@RequiredArgsConstructor
@Transactional
public class ActualAccountService implements ActualAccountUseCase {
    private static final String POLICY = "ACTUAL_KRW_V1";
    private final ActualAccountPort port;
    private final ObjectMapper mapper;

    @Override @Transactional(readOnly = true)
    public List<Account> list(long memberId) { return port.list(memberId).stream().map(Account::from).toList(); }
    @Override @Transactional(readOnly = true)
    public Account get(long memberId, long id) {
        return Account.from(port.owned(memberId, id).orElseThrow(() -> new GlobalException(ErrorCode.RESOURCE_NOT_FOUND)));
    }

    @Override public Mutation create(Create command) {
        port.lockMember(command.memberId());
        ActualAccountRules.key(command.key());
        ActualAccount account = ActualAccount.create(command.memberId(), command.name(), command.brokerLabel(), command.baseCurrency());
        String fingerprint = fingerprint(command.memberId(), 0, "CREATE_ACCOUNT", null,
                new CanonicalCreate(account.getName(), account.getBaseCurrency(), account.getBrokerLabel()));
        var receipt = port.receipt(command.memberId(), 0, "CREATE_ACCOUNT", command.key());
        if (receipt.isPresent()) return replay(receipt.get(), fingerprint, command.memberId());
        port.saveAndFlush(account);
        if (account.getId() > ActualAccountRules.MAX_SAFE_INTEGER) throw new GlobalException(ErrorCode.ACTUAL_NUMERIC_RANGE_EXCEEDED);
        Mutation result = new Mutation(Account.from(account), LocalDateTime.now(), true);
        port.saveReceipt(ActualMutationReceipt.success(command.memberId(), 0, account.getId(), "CREATE_ACCOUNT",
                command.key(), fingerprint, json(result)));
        return result;
    }

    @Override public Mutation rename(Rename command) {
        ActualAccount account = port.lockOwned(command.memberId(), command.accountId())
                .orElseThrow(() -> new GlobalException(ErrorCode.RESOURCE_NOT_FOUND));
        ActualAccountRules.key(command.key());
        long expected = ActualAccountRules.ifMatch(command.ifMatch());
        String name = ActualAccountRules.text(command.name(), 50, false);
        String fingerprint = fingerprint(command.memberId(), command.accountId(), "RENAME_ACCOUNT", command.ifMatch(), new CanonicalRename(name));
        var receipt = port.receipt(command.memberId(), command.accountId(), "RENAME_ACCOUNT", command.key());
        if (receipt.isPresent()) return replay(receipt.get(), fingerprint, command.memberId());
        if (expected != account.getAccountVersion()) throw new GlobalException(ErrorCode.ACTUAL_VERSION_CONFLICT);
        account.rename(name);
        port.saveAndFlush(account);
        Mutation result = new Mutation(Account.from(account), LocalDateTime.now(), false);
        port.saveReceipt(ActualMutationReceipt.success(command.memberId(), command.accountId(), account.getId(),
                "RENAME_ACCOUNT", command.key(), fingerprint, json(result)));
        return result;
    }

    private Mutation replay(ActualMutationReceipt receipt, String fingerprint, long memberId) {
        // Recheck the response resource ownership before returning an old success, including create replay.
        port.owned(memberId, receipt.getResponseAccountId()).orElseThrow(() -> new GlobalException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!receipt.getRequestFingerprint().equals(fingerprint)) throw new GlobalException(ErrorCode.ACTUAL_IDEMPOTENCY_CONFLICT);
        try { return mapper.readValue(receipt.getResponseBody(), Mutation.class); }
        catch (JsonProcessingException ignored) { throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR); }
    }

    private String fingerprint(long memberId, long scope, String operation, String ifMatch, Object body) {
        String canonical = json(new Fingerprint(memberId, scope, operation, POLICY, null, ifMatch, body));
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ignored) { throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR); }
    }
    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException ignored) { throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR); }
    }
    private record Fingerprint(long memberId, long accountId, String operation, String policyVersion,
                               Long targetEntryId, String originalIfMatch, Object body) { }
    private record CanonicalCreate(String name, String baseCurrency, String brokerLabel) { }
    private record CanonicalRename(String name) { }
}
