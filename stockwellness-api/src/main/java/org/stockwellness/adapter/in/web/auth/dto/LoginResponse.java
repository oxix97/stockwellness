package org.stockwellness.adapter.in.web.auth.dto;

import java.time.LocalDate;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Long memberId,
        String email,
        String nickname,
        LocalDate joinedDate
) {}
