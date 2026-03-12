package org.stockwellness.application.service;
import org.springframework.stereotype.Service;

@Service
public class TestService {
    public void execute() {}

    public String executeWithArgs(String s, int i) {
        return s + " " + i;
    }

    public void throwException() {
        throw new RuntimeException("Test Exception");
    }
}
