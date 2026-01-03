package org.stockwellness.adapter.in.web.portfolio.dto;

import java.util.List;

public record PortfolioUpdateRequest(List<PortfolioItemRequest> items) {}
