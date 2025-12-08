package org.stockwellness.application.port.out;

public interface ValidateTokenPort {
    Long extractMemberId(String token);
    boolean isTokenValid(String token);
}