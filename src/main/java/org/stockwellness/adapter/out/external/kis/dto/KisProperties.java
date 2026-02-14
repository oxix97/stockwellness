package org.stockwellness.adapter.out.external.kis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kis")
public record KisProperties(
    String baseUrl,
    String appKey,
    String appSecret
) {}