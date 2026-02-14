package org.stockwellness.adapter.out.persistence.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.PopularSearchPort;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PopularSearchRedisAdapter implements PopularSearchPort {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "search:rank:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int RANK_SIZE = 10;
    private static final Duration KEY_TTL = Duration.ofDays(2); // 2일 후 자동 삭제

    @Override
    public void incrementCount(String keyword) {
        String key = generateKey(LocalDate.now());
        redisTemplate.opsForZSet().incrementScore(key, keyword, 1.0);
        redisTemplate.expire(key, KEY_TTL);
    }

    @Override
    public List<String> findTop10() {
        String key = generateKey(LocalDate.now());
        Set<String> top10 = redisTemplate.opsForZSet().reverseRange(key, 0, RANK_SIZE - 1);
        
        if (top10 == null) {
            return Collections.emptyList();
        }
        
        return top10.stream().collect(Collectors.toList());
    }

    private String generateKey(LocalDate date) {
        return KEY_PREFIX + date.format(DATE_FORMATTER);
    }
}
