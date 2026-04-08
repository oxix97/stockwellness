package org.stockwellness.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class RateLimiterTestConfig {

    @Bean
    @Primary
    public RateLimiter kisRateLimiter() {
        return RateLimiter.ofDefaults("kisRateLimiter");
    }
}
