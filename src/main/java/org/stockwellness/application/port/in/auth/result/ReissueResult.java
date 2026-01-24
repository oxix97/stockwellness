package org.stockwellness.application.port.in.auth.result;

public record ReissueResult(
    String accessToken,
    String refreshToken
) {}
