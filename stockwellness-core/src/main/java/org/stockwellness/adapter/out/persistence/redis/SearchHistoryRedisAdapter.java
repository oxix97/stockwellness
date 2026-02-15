package org.stockwellness.adapter.out.persistence.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.stock.SearchHistoryPort;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SearchHistoryRedisAdapter implements SearchHistoryPort {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "search:history:";
    private static final int MAX_HISTORY_SIZE = 10;

    @Override
    public void save(Long memberId, String keyword) {
        String key = generateKey(memberId);
        double timestamp = (double) System.currentTimeMillis();

        redisTemplate.opsForZSet().add(key, keyword, timestamp);
        
        Long size = redisTemplate.opsForZSet().size(key);
        if (size != null && size > MAX_HISTORY_SIZE) {
            redisTemplate.opsForZSet().removeRange(key, 0, size - MAX_HISTORY_SIZE - 1);
        }
    }

    @Override
    public List<String> findAll(Long memberId) {
        String key = generateKey(memberId);
        Set<String> history = redisTemplate.opsForZSet().reverseRange(key, 0, MAX_HISTORY_SIZE - 1);
        
        if (history == null) {
            return Collections.emptyList();
        }
        
        return history.stream().collect(Collectors.toList());
    }

    @Override
    public void delete(Long memberId, String keyword) {
        String key = generateKey(memberId);
        redisTemplate.opsForZSet().remove(key, keyword);
    }

    @Override
    public void deleteAll(Long memberId) {
        String key = generateKey(memberId);
        redisTemplate.delete(key);
    }

    @Override
    public void setExpireTime(Long memberId, Duration duration) {
        String key = generateKey(memberId);
        redisTemplate.expire(key, duration);
    }

    private String generateKey(Long memberId) {
        return KEY_PREFIX + memberId;
    }
}
