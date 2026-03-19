package org.stockwellness.application.port.in.auth.result;

import java.time.LocalDate;

public record LoginResult(
    String accessToken,
    String refreshToken,
    Long memberId,
    String email,
    String nickname,
    LocalDate joinedDate
) {}
