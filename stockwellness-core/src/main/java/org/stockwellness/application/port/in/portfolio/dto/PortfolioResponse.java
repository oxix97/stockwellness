package org.stockwellness.application.port.in.portfolio.dto;

import org.stockwellness.domain.portfolio.Portfolio;
import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(
        Long id,
        String name,
        String description,
        BigDecimal totalPurchaseAmount,
        List<PortfolioItemResponse> items
) {
    public static PortfolioResponse from(Portfolio entity) {
        return new PortfolioResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.calculateTotalPurchaseAmount(),
                entity.getItems().stream().map(PortfolioItemResponse::from).toList()
        );
    }
}
