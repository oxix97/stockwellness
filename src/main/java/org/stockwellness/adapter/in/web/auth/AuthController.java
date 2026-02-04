package org.stockwellness.adapter.in.web.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.stockwellness.adapter.in.web.auth.dto.LoginRequest;
import org.stockwellness.adapter.in.web.auth.dto.LoginResponse;
import org.stockwellness.adapter.in.web.auth.dto.ReissueRequest;
import org.stockwellness.adapter.in.web.auth.dto.ReissueResponse;
import org.stockwellness.application.port.in.auth.AuthUseCase;
import org.stockwellness.application.port.in.auth.command.LoginCommand;
import org.stockwellness.application.port.in.auth.result.LoginResult;
import org.stockwellness.application.port.in.auth.result.ReissueResult;
import org.stockwellness.global.security.MemberPrincipal;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginCommand command = new LoginCommand(request.email(), request.nickname(), request.loginType());
        LoginResult result = authUseCase.login(command);
        
        LoginResponse response = new LoginResponse(
            result.accessToken(),
            result.refreshToken(),
            result.memberId(),
            result.email(),
            result.nickname()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reissue")
    public ResponseEntity<ReissueResponse> reissue(@Valid @RequestBody ReissueRequest request) {
        ReissueResult result = authUseCase.reissue(request.refreshToken());
        
        ReissueResponse response = new ReissueResponse(
            result.accessToken(),
            result.refreshToken()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal MemberPrincipal memberPrincipal) {
        authUseCase.logout(memberPrincipal.id());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/test")
    public Map<String, String> test() {
        return Map.of("status", "ok", "service", "stockwellness");
    }
}