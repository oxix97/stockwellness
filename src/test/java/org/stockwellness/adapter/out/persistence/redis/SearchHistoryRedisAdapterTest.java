package org.stockwellness.adapter.out.persistence.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import org.stockwellness.application.port.out.stock.SearchHistoryPort;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("최근 검색어 Redis 저장소 테스트")
class SearchHistoryRedisAdapterTest {

    private SearchHistoryPort searchHistoryRedisAdapter;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        searchHistoryRedisAdapter = new SearchHistoryRedisAdapter(redisTemplate);
    }

    @Test
    @DisplayName("성공: 최근 검색어를 저장하고 최대 개수를 초과하면 오래된 검색어를 삭제한다")
    void save_search_history() {
        // given
        Long memberId = 1L;
        String keyword = "삼성전자";
        String key = "search:history:1";
        
        given(zSetOperations.size(key)).willReturn(11L);

        // when
        searchHistoryRedisAdapter.save(memberId, keyword);

        // then
        verify(zSetOperations).add(eq(key), eq(keyword), anyDouble());
        verify(zSetOperations).removeRange(eq(key), eq(0L), eq(0L)); // size(11) - max(10) - 1 = 0
    }

    @Test
    @DisplayName("성공: 특정 사용자의 최근 검색어 목록을 최신순으로 조회한다")
    void find_all_search_history() {
        // given
        Long memberId = 1L;
        String key = "search:history:1";
        Set<String> historySet = new LinkedHashSet<>(List.of("애플", "삼성전자"));
        
        given(zSetOperations.reverseRange(key, 0, 9)).willReturn(historySet);

        // when
        List<String> result = searchHistoryRedisAdapter.findAll(memberId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo("애플");
        assertThat(result.get(1)).isEqualTo("삼성전자");
    }

    @Test
    @DisplayName("성공: 특정 검색어를 기록에서 삭제한다")
    void delete_search_history() {
        // given
        Long memberId = 1L;
        String keyword = "삼성전자";
        String key = "search:history:1";

        // when
        searchHistoryRedisAdapter.delete(memberId, keyword);

        // then
        verify(zSetOperations).remove(key, keyword);
    }

    @Test
    @DisplayName("성공: 검색어 기록이 없을 경우 빈 리스트를 반환한다")
    void find_all_empty_history() {
        // given
        Long memberId = 1L;
        String key = "search:history:1";
        given(zSetOperations.reverseRange(key, 0, 9)).willReturn(null);

        // when
        List<String> result = searchHistoryRedisAdapter.findAll(memberId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("성공: 키의 만료 시간을 설정한다")
    void set_expire_time() {
        // given
        Long memberId = 1L;
        String key = "search:history:1";
        Duration duration = Duration.ofDays(30);

        // when
        searchHistoryRedisAdapter.setExpireTime(memberId, duration);

        // then
        verify(redisTemplate).expire(key, duration);
    }
}
