package org.stockwellness.adapter.out.persistence.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import org.stockwellness.application.port.out.stock.PopularSearchPort;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("인기 검색어 Redis 저장소 테스트")
class PopularSearchRedisAdapterTest {

    private PopularSearchPort popularSearchRedisAdapter;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        popularSearchRedisAdapter = new PopularSearchRedisAdapter(redisTemplate);
    }

    @Test
    @DisplayName("성공: 검색 빈도를 증가시키고 TTL을 설정한다")
    void increment_count() {
        // given
        String keyword = "삼성전자";
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = "search:rank:" + today;

        // when
        popularSearchRedisAdapter.incrementCount(keyword);

        // then
        verify(zSetOperations).incrementScore(eq(key), eq(keyword), eq(1.0));
        verify(redisTemplate).expire(eq(key), any());
    }

    @Test
    @DisplayName("성공: 오늘 날짜의 인기 검색어 Top 10을 조회한다")
    void find_top_10() {
        // given
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = "search:rank:" + today;
        Set<String> rankSet = new LinkedHashSet<>(List.of("삼성전자", "애플", "카카오"));
        
        given(zSetOperations.reverseRange(key, 0, 9)).willReturn(rankSet);

        // when
        List<String> result = popularSearchRedisAdapter.findTop10();

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo("삼성전자");
        assertThat(result.get(1)).isEqualTo("애플");
        assertThat(result.get(2)).isEqualTo("카카오");
    }
}
