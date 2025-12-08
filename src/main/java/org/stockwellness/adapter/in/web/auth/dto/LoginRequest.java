package org.stockwellness.adapter.in.web.auth.dto;

import jakarta.validation.constraints.Email;
import org.stockwellness.domain.member.LoginType;

public record LoginRequest(
        @Email String email,
        String nickname,
        LoginType loginType
) {
}
