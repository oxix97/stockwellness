package org.stockwellness.global.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Getter
@RequiredArgsConstructor
public enum CacheType {
    // 인증/회원
    MEMBER("member", Duration.ofMinutes(30), true), // Security Serializer 필요 시 플래그 활용

    // 주식 도메인
    STOCK_INFO("stock_info", Duration.ofDays(7), false),
    STOCK_PRICES("stock_prices", Duration.ofDays(7), false),
    AI_ANALYSIS("ai_analysis", Duration.ofHours(24), false),

    // 섹터 도메인
    SECTOR_RANKING("sectorRanking", Duration.ofHours(24), false),
    SECTOR_SUPPLY("sectorSupply", Duration.ofHours(24), false),
    SECTOR_DETAIL("sectorDetail", Duration.ofHours(24), false);

    private final String cacheName;
    private final Duration ttl;
    private final boolean useSecuritySerializer;
}
