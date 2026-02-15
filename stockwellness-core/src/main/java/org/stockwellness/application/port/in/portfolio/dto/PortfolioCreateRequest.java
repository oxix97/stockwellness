package org.stockwellness.application.port.in.portfolio.dto;

import java.util.List;

public record PortfolioCreateRequest(String name, String description, List<PortfolioItemRequest> items) {}