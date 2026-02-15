package org.stockwellness.application.port.in.auth.dto;

import jakarta.validation.constraints.Email;
import org.stockwellness.domain.member.LoginType;

public record LoginRequest(
        @Email String email,
        String nickname,
        LoginType loginType
) {
}
