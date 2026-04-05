package org.stockwellness.integration.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.stockwellness.application.port.in.auth.dto.LoginRequest;
import org.stockwellness.domain.member.LoginType;
import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.stockwellness.application.port.out.auth.RefreshTokenPort;
import org.stockwellness.adapter.out.persistence.stock.StockPriceCacheAdapter;
import org.stockwellness.application.port.out.stock.PopularSearchPort;

/**
 * 모든 API 통합 테스트의 공통 설정을 관리하는 베이스 클래스.
 * 인프라(Redis, Kafka) 연결을 차단하고 비즈니스 흐름을 검증합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // --- 인프라 레벨 Mock (실제 연결 차단) ---
    @MockitoBean
    protected org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory redisConnectionFactory;

    @MockitoBean
    protected KafkaTemplate<String, Object> kafkaTemplate;

    // --- 비즈니스/어댑터 레벨 Mock (Redis 의존성 제거) ---
    @MockitoBean
    protected StringRedisTemplate redisTemplate;

    @MockitoBean
    protected RefreshTokenPort refreshTokenPort;

    @MockitoBean
    protected StockPriceCacheAdapter stockPriceCacheAdapter;

    @MockitoBean
    protected PopularSearchPort popularSearchPort;

    @MockitoBean
    protected org.springframework.cache.CacheManager cacheManager;

    @BeforeEach
    void setCommonMock() {
        // Redis NPE 방지를 위한 기본 Mock 설정
        org.springframework.data.redis.core.ValueOperations<String, String> valueOperations = mock(org.springframework.data.redis.core.ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        org.springframework.data.redis.core.ZSetOperations<String, String> zSetOperations = mock(org.springframework.data.redis.core.ZSetOperations.class);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);

        org.springframework.data.redis.core.HashOperations<String, Object, Object> hashOperations = mock(org.springframework.data.redis.core.HashOperations.class);
        given(redisTemplate.opsForHash()).willReturn(hashOperations);

        org.springframework.data.redis.core.ListOperations<String, String> listOperations = mock(org.springframework.data.redis.core.ListOperations.class);
        given(redisTemplate.opsForList()).willReturn(listOperations);

        org.springframework.data.redis.core.SetOperations<String, String> setOperations = mock(org.springframework.data.redis.core.SetOperations.class);
        given(redisTemplate.opsForSet()).willReturn(setOperations);

        // Cache NPE 방지
        org.springframework.cache.Cache cache = mock(org.springframework.cache.Cache.class);
        given(cacheManager.getCache(org.mockito.ArgumentMatchers.anyString())).willReturn(cache);
    }

    /**
     * 테스트용 사용자 로그인을 수행하고 Access Token을 반환합니다.
     */
    protected String loginAndGetToken(String email, String nickname) throws Exception {
        LoginRequest request = new LoginRequest(email, nickname, LoginType.KAKAO);
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andReturn().getResponse().getContentAsString();
        
        var node = objectMapper.readTree(response).get("data");
        if (node == null || node.get("accessToken") == null) {
            throw new RuntimeException("로그인 실패: " + response);
        }
        return node.get("accessToken").asText();
    }
}
