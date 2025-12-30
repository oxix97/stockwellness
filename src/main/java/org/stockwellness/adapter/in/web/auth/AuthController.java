package org.stockwellness.adapter.in.web.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.stockwellness.adapter.in.web.auth.dto.LoginRequest;
import org.stockwellness.adapter.in.web.auth.dto.LoginResponse;
import org.stockwellness.adapter.in.web.auth.dto.ReissueRequest;
import org.stockwellness.adapter.in.web.auth.dto.ReissueResponse;
import org.stockwellness.application.service.AuthService;
import org.stockwellness.domain.member.Member;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reissue")
    public ResponseEntity<ReissueResponse> reissue(@Valid @RequestBody ReissueRequest request) {
        ReissueResponse response = authService.reissue(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Member member) {  // JwtAuthenticationFilter에서 Member principal
        authService.logout(member.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/test")
    public Map<String, String> test() {
        return Map.of("status", "ok", "service", "stockwellness");
    }
}
