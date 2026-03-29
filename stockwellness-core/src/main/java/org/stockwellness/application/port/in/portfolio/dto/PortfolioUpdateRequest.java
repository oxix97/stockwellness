package org.stockwellness.application.port.in.portfolio.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PortfolioUpdateRequest(
    String name,
    String description,
    @NotEmpty(message = "포트폴리오에 최소 1개 이상의 종목이 필요합니다.") List<PortfolioItemRequest> items
) {}
