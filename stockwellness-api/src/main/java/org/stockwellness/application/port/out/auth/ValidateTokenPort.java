package org.stockwellness.application.port.out.auth;

import io.jsonwebtoken.JwtException;

public interface ValidateTokenPort {
    Long validateAndGetId(String token) throws JwtException;
}