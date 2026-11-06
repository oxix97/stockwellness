package org.stockwellness.adapter.in.web.actual;

import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.application.port.in.actual.ActualAccountUseCase;
import org.stockwellness.application.port.in.actual.ActualAccountUseCase.Account;
import org.stockwellness.application.port.in.actual.ActualAccountUseCase.Mutation;
import org.stockwellness.domain.actual.ActualAccountRules;
import org.stockwellness.global.common.response.ApiResponse;
import org.stockwellness.global.common.response.SuccessCode;
import org.stockwellness.global.security.MemberPrincipal;

@RestController
@RequestMapping("/api/v1/investment-accounts")
@RequiredArgsConstructor
public class ActualAccountController {
    private final ActualAccountUseCase useCase;
    @GetMapping public ApiResponse<List<Account>> list(@AuthenticationPrincipal MemberPrincipal member) {
        return ApiResponse.success(useCase.list(member.id()));
    }
    @GetMapping("/{accountId}") public ApiResponse<Account> get(@AuthenticationPrincipal MemberPrincipal member,
            @PathVariable String accountId) {
        return ApiResponse.success(useCase.get(member.id(), ActualAccountRules.id(accountId)));
    }
    @PostMapping public ResponseEntity<ApiResponse<Account>> create(@AuthenticationPrincipal MemberPrincipal member,
            @RequestBody(required = false) String body, @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        var input = ActualAccountJson.create(body);
        return response(useCase.create(new ActualAccountUseCase.Create(member.id(), input.name(), input.baseCurrency(), input.brokerLabel(), key)));
    }
    @PatchMapping("/{accountId}") public ResponseEntity<ApiResponse<Account>> rename(@AuthenticationPrincipal MemberPrincipal member,
            @PathVariable String accountId, @RequestBody(required = false) String body,
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        long id = ActualAccountRules.id(accountId);
        useCase.get(member.id(), id); // Ownership takes priority over strict body/header details.
        return response(useCase.rename(new ActualAccountUseCase.Rename(member.id(), id, ActualAccountJson.rename(body), ifMatch, key)));
    }
    private ResponseEntity<ApiResponse<Account>> response(Mutation mutation) {
        SuccessCode code = mutation.created() ? SuccessCode.CREATED : SuccessCode.OK;
        return ResponseEntity.status(code.getStatus()).body(new ApiResponse<>(true, code.getStatusCode(), code.getCode(),
                code.getMessage(), mutation.data(), mutation.timestamp(), null, Collections.emptyList()));
    }
}
