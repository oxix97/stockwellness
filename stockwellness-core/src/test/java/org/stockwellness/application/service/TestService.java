package org.stockwellness.application.service;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;

@Service
public class TestService {
    public void execute() {}

    public String executeWithArgs(String s, int i) {
        return s + " " + i;
    }

    public void executeWithHeader(@RequestHeader("X-Test-Token") String token, String payload) {}

    public void throwException() {
        throw new RuntimeException("Test Exception");
    }
}
