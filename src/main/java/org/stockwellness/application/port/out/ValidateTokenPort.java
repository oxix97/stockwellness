package org.stockwellness.application.port.out;

import io.jsonwebtoken.JwtException;

public interface ValidateTokenPort {
    Long validateAndGetId(String token) throws JwtException;
}