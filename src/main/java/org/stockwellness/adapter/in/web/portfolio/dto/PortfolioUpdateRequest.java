package org.stockwellness.adapter.in.web.portfolio.dto;

import java.util.List;

public record PortfolioUpdateRequest(
    String name,
    String description,
    List<PortfolioItemRequest> items
) {}
