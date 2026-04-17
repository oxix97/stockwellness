package org.stockwellness.global.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Getter
@RequiredArgsConstructor
public enum CacheType {
    // 인증/회원
    MEMBER("member:v2", Duration.ofMinutes(30), true), // Security Serializer 필요 시 플래그 활용

    // 주식 도메인
    AI_ANALYSIS("ai_analysis:v2", Duration.ofHours(24), false),

    // 섹터 도메인
    SECTOR_RANKING("sectorRanking:v3", Duration.ofHours(24), false),
    SECTOR_SUPPLY("sectorSupply:v3", Duration.ofHours(24), false),
    SECTOR_DETAIL("sectorDetail:v3", Duration.ofHours(24), false),
    SECTOR_COMPARISON("sectorComparison:v3", Duration.ofHours(24), false);

    private final String cacheName;
    private final Duration ttl;
    private final boolean useSecuritySerializer;
}
